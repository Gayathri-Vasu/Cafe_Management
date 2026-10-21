package com.cafe.management.service;

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

public interface AuthService {

    /** Verifies email + password against MySQL and returns a signed JWT. */
    LoginResponse login(LoginRequest request);

    /** Verifies a staff code + numeric PIN (counter badge sign-in). */
    LoginResponse loginWithPin(PinLoginRequest request);

    /** Texts a one-time code to the number linked to an account. */
    OtpRequestResponse requestOtp(OtpRequestBody request);

    /** Exchanges a valid one-time code for a signed JWT. */
    LoginResponse verifyOtp(OtpVerifyRequest request);

    /** Verifies a Google ID token and returns a signed JWT. */
    LoginResponse loginWithGoogle(SocialLoginRequest request);

    /** Verifies an Apple identity token and returns a signed JWT. */
    LoginResponse loginWithApple(SocialLoginRequest request);

    /** Reports which sign-in methods the server can currently complete. */
    AuthProvidersResponse providers();

    /** Returns the profile of the currently authenticated user. */
    UserDto currentUser();

    /** Changes the password of the currently authenticated user. */
    void changePassword(ChangePasswordRequest request);

    /** Sets or replaces the counter PIN of the currently authenticated user. */
    void setPin(SetPinRequest request);

    /** Sets or replaces the mobile number of the currently authenticated user. */
    void setPhone(SetPhoneRequest request);

    /** Updates the personal details of the currently authenticated user. */
    UserDto updateProfile(UpdateProfileRequest request);

    /**
     * Changes the email and staff code of the currently authenticated user.
     * Administrator only.
     *
     * <p>Returns a full login response because the JWT is keyed on the email -
     * the caller's existing token stops working the moment it changes.</p>
     */
    LoginResponse updateLoginIds(UpdateLoginIdsRequest request);
}
