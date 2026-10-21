package com.cafe.management.controller;

import com.cafe.management.dto.ApiMessage;
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
import com.cafe.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * <p><b>Public</b> - reachable without a token, because that is how a session is
 * obtained in the first place:</p>
 * <ul>
 *   <li>{@code POST /api/auth/login} - email + password.</li>
 *   <li>{@code POST /api/auth/pin-login} - staff code + counter PIN.</li>
 *   <li>{@code POST /api/auth/otp/request} - text a one-time code to a phone.</li>
 *   <li>{@code POST /api/auth/otp/verify} - exchange that code for a token.</li>
 *   <li>{@code POST /api/auth/google} - exchange a Google ID token.</li>
 *   <li>{@code POST /api/auth/apple} - exchange an Apple identity token.</li>
 *   <li>{@code GET  /api/auth/providers} - which of the above are switched on.</li>
 * </ul>
 *
 * <p><b>Authenticated</b> - require an existing token:</p>
 * <ul>
 *   <li>{@code GET  /api/auth/me} - the logged-in user's profile.</li>
 *   <li>{@code POST /api/auth/change-password}</li>
 *   <li>{@code POST /api/auth/pin} - set the counter PIN.</li>
 *   <li>{@code POST /api/auth/phone} - link a mobile number for OTP sign-in.</li>
 *   <li>{@code PUT  /api/auth/profile} - update the personal details.</li>
 *   <li>{@code PUT  /api/auth/login-ids} - change the email and staff code (ADMIN only).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/pin-login")
    public ResponseEntity<LoginResponse> pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithPin(request));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequestBody request) {
        return ResponseEntity.ok(authService.requestOtp(request));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> google(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> apple(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithApple(request));
    }

    @GetMapping("/providers")
    public ResponseEntity<AuthProvidersResponse> providers() {
        return ResponseEntity.ok(authService.providers());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.currentUser());
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiMessage> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(new ApiMessage("Password changed successfully"));
    }

    @PostMapping("/pin")
    public ResponseEntity<ApiMessage> setPin(@Valid @RequestBody SetPinRequest request) {
        authService.setPin(request);
        return ResponseEntity.ok(new ApiMessage("Counter PIN saved successfully"));
    }

    @PostMapping("/phone")
    public ResponseEntity<ApiMessage> setPhone(@Valid @RequestBody SetPhoneRequest request) {
        authService.setPhone(request);
        return ResponseEntity.ok(new ApiMessage("Phone number saved successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(request));
    }

    /**
     * Changes the email and staff code of the signed-in administrator.
     *
     * <p>Administrator only. A fresh token comes back in the response because
     * the old one was signed against the previous email.</p>
     */
    @PutMapping("/login-ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoginResponse> updateLoginIds(@Valid @RequestBody UpdateLoginIdsRequest request) {
        return ResponseEntity.ok(authService.updateLoginIds(request));
    }
}
