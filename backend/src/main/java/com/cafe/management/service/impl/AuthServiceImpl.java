package com.cafe.management.service.impl;

import com.cafe.management.dto.AuthProvidersResponse;
import com.cafe.management.dto.ChangePasswordRequest;
import com.cafe.management.dto.LoginRequest;
import com.cafe.management.dto.LoginResponse;
import com.cafe.management.dto.OtpRequestBody;
import com.cafe.management.dto.OtpRequestResponse;
import com.cafe.management.dto.OtpVerifyRequest;
import com.cafe.management.dto.PinLoginRequest;
import com.cafe.management.dto.SetPhoneRequest;
import com.cafe.management.dto.SetPinRequest;
import com.cafe.management.dto.SocialLoginRequest;
import com.cafe.management.dto.UpdateLoginIdsRequest;
import com.cafe.management.dto.UpdateProfileRequest;
import com.cafe.management.dto.UserDto;
import com.cafe.management.entity.Role;
import com.cafe.management.entity.User;
import com.cafe.management.entity.UserStatus;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.DuplicateResourceException;
import com.cafe.management.exception.InvalidCredentialsException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.UserRepository;
import com.cafe.management.security.AttemptThrottle;
import com.cafe.management.security.CurrentUser;
import com.cafe.management.security.IdTokenVerifier;
import com.cafe.management.security.JwtService;
import com.cafe.management.service.AuthService;
import com.cafe.management.service.OtpService;
import com.cafe.management.util.DtoMapper;
import com.cafe.management.util.PhoneNumbers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Every way a user can sign in.
 *
 * <p>Whichever route is taken - password, staff PIN, SMS code, Google or Apple -
 * the outcome is the same: the account is loaded from MySQL, checked for being
 * active, and handed to {@link JwtService} so the rest of the application sees
 * one identical JWT. No other component needs to know how the session started.</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> GOOGLE_ISSUERS =
            List.of("https://accounts.google.com", "accounts.google.com");

    private static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final List<String> APPLE_ISSUERS = List.of("https://appleid.apple.com");

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final OtpService otpService;
    private final IdTokenVerifier idTokenVerifier;
    private final AttemptThrottle throttle;

    private final String googleClientId;
    private final String appleClientId;
    private final int otpLength;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           CurrentUser currentUser,
                           OtpService otpService,
                           IdTokenVerifier idTokenVerifier,
                           AttemptThrottle throttle,
                           @Value("${app.social.google.client-id:}") String googleClientId,
                           @Value("${app.social.apple.client-id:}") String appleClientId,
                           @Value("${app.otp.length:6}") int otpLength) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.otpService = otpService;
        this.idTokenVerifier = idTokenVerifier;
        this.throttle = throttle;
        this.googleClientId = googleClientId;
        this.appleClientId = appleClientId;
        this.otpLength = otpLength;
    }

    // ------------------------------------------------------------------
    // Email + password
    // ------------------------------------------------------------------

    @Override
    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException / DisabledException, both handled globally.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User account no longer exists"));

        return issueToken(user);
    }

    // ------------------------------------------------------------------
    // Staff code + PIN
    // ------------------------------------------------------------------

    @Override
    public LoginResponse loginWithPin(PinLoginRequest request) {
        String staffCode = request.staffCode().trim();
        String throttleKey = "pin:" + staffCode;
        throttle.checkAllowed(throttleKey);

        Optional<User> found = userRepository.findByStaffCode(staffCode);
        if (found.isEmpty()) {
            throttle.recordFailure(throttleKey);
            throw new InvalidCredentialsException("That staff code and PIN do not match");
        }

        User user = found.get();
        if (user.getPinHash() == null || !passwordEncoder.matches(request.pin(), user.getPinHash())) {
            throttle.recordFailure(throttleKey);
            throw new InvalidCredentialsException(
                    user.getPinHash() == null
                            ? "No counter PIN has been set for this staff code yet"
                            : "That staff code and PIN do not match");
        }

        requireActive(user);
        throttle.reset(throttleKey);
        return issueToken(user);
    }

    // ------------------------------------------------------------------
    // Phone + one-time code
    // ------------------------------------------------------------------

    @Override
    public OtpRequestResponse requestOtp(OtpRequestBody request) {
        String phone = PhoneNumbers.normalize(request.phone());
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException("No account is linked to that phone number"));

        requireActive(user);
        otpService.request(phone);
        return new OtpRequestResponse(otpService.ttlSeconds(), PhoneNumbers.mask(phone));
    }

    @Override
    public LoginResponse verifyOtp(OtpVerifyRequest request) {
        String phone = PhoneNumbers.normalize(request.phone());
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new InvalidCredentialsException("That code is not correct"));

        otpService.verify(phone, request.code());
        requireActive(user);
        return issueToken(user);
    }

    // ------------------------------------------------------------------
    // Google / Apple
    // ------------------------------------------------------------------

    @Override
    public LoginResponse loginWithGoogle(SocialLoginRequest request) {
        requireConfigured(googleClientId, "Google");
        IdTokenVerifier.VerifiedToken identity = idTokenVerifier.verify(
                request.idToken(), GOOGLE_JWKS_URI, GOOGLE_ISSUERS, googleClientId);

        return loginWithFederatedIdentity(identity, "Google",
                userRepository::findByGoogleSubject, User::setGoogleSubject);
    }

    @Override
    public LoginResponse loginWithApple(SocialLoginRequest request) {
        requireConfigured(appleClientId, "Apple");
        IdTokenVerifier.VerifiedToken identity = idTokenVerifier.verify(
                request.idToken(), APPLE_JWKS_URI, APPLE_ISSUERS, appleClientId);

        return loginWithFederatedIdentity(identity, "Apple",
                userRepository::findByAppleSubject, User::setAppleSubject);
    }

    /**
     * Resolves a verified provider identity to a local account.
     *
     * <p>Matching is done on the provider subject first, because Apple hides the
     * real address behind a private relay. When the subject is unknown the
     * verified email is used once and the subject is then recorded on the account,
     * so later sign-ins no longer depend on the email.</p>
     *
     * <p>An unknown email is rejected rather than auto-provisioned: this is a
     * closed staff system, and anyone may own a Google account.</p>
     */
    private LoginResponse loginWithFederatedIdentity(IdTokenVerifier.VerifiedToken identity,
                                                     String providerName,
                                                     Function<String, Optional<User>> findBySubject,
                                                     BiConsumer<User, String> linkSubject) {
        Optional<User> alreadyLinked = findBySubject.apply(identity.subject());
        if (alreadyLinked.isPresent()) {
            User user = alreadyLinked.get();
            requireActive(user);
            return issueToken(user);
        }

        if (identity.email() == null || identity.email().isBlank()) {
            throw new InvalidCredentialsException("Your " + providerName
                    + " account did not share an email address, so no cafe account could be matched.");
        }

        User user = userRepository.findByEmailIgnoreCase(identity.email())
                .orElseThrow(() -> new InvalidCredentialsException("No cafe account uses "
                        + identity.email() + ". Ask an administrator to create one, then sign in again."));

        requireActive(user);
        linkSubject.accept(user, identity.subject());
        userRepository.save(user);
        return issueToken(user);
    }

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    @Override
    public AuthProvidersResponse providers() {
        boolean googleReady = isConfigured(googleClientId);
        boolean appleReady = isConfigured(appleClientId);

        return new AuthProvidersResponse(
                true,
                true,
                otpService.isEnabled(),
                googleReady,
                appleReady,
                googleReady ? googleClientId : null,
                appleReady ? appleClientId : null,
                otpLength,
                otpService.smsProvider());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto currentUser() {
        return DtoMapper.toUserDto(currentUser.get());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUser.get();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setPin(SetPinRequest request) {
        User user = currentUser.get();
        requirePassword(user, request.currentPassword());

        if (user.getPinHash() != null && passwordEncoder.matches(request.pin(), user.getPinHash())) {
            throw new BadRequestException("Choose a PIN that is different from your current one");
        }

        user.setPinHash(passwordEncoder.encode(request.pin()));
        userRepository.save(user);
        throttle.reset("pin:" + user.getStaffCode());
    }

    @Override
    @Transactional
    public void setPhone(SetPhoneRequest request) {
        User user = currentUser.get();
        requirePassword(user, request.currentPassword());

        String phone = PhoneNumbers.normalize(request.phone());
        Optional<User> owner = userRepository.findByPhone(phone);
        if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
            throw new DuplicateResourceException("That phone number is already linked to another account");
        }

        user.setPhone(phone);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = currentUser.get();

        user.setName(request.name().trim());
        user.setLastName(trimToNull(request.lastName()));
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPlace(trimToNull(request.place()));
        user.setAddress(trimToNull(request.address()));

        // An empty number clears the contact number rather than storing "".
        String phone = PhoneNumbers.normalize(request.phone());
        if (phone.isEmpty()) {
            user.setPhone(null);
        } else {
            Optional<User> owner = userRepository.findByPhone(phone);
            if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
                throw new DuplicateResourceException("That phone number is already linked to another account");
            }
            user.setPhone(phone);
        }

        userRepository.save(user);
        return DtoMapper.toUserDto(user);
    }

    // ------------------------------------------------------------------
    // Sign-in identifiers (administrator only)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public LoginResponse updateLoginIds(UpdateLoginIdsRequest request) {
        User user = currentUser.get();

        // The endpoint is already restricted to administrators; this keeps the
        // rule true even if the endpoint is ever reached from somewhere else.
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only an administrator can change the email or staff code");
        }

        String email = request.email().trim().toLowerCase();
        String staffCode = request.staffCode().trim();

        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists");
        }

        Optional<User> badgeOwner = userRepository.findByStaffCode(staffCode);
        if (badgeOwner.isPresent() && !badgeOwner.get().getId().equals(user.getId())) {
            throw new DuplicateResourceException("Staff code '" + staffCode + "' is already used by another account");
        }

        user.setEmail(email);
        user.setStaffCode(staffCode);
        userRepository.save(user);

        // The token subject is the email, so the caller must be handed a token
        // that matches the new value or their very next request would 401.
        return issueToken(user);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Treats a blank optional field as "not provided". */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LoginResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, "Bearer", user.getId(), user.getName(),
                user.getEmail(), user.getRole().name());
    }

    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("This account has been disabled. Please contact an administrator.");
        }
    }

    private void requirePassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
    }

    private void requireConfigured(String clientId, String providerName) {
        if (!isConfigured(clientId)) {
            throw new BadRequestException(providerName + " sign-in is not enabled on this server");
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}
