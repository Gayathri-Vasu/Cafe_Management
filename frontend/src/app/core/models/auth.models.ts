/** The only two roles that exist in this internal system. */
export type Role = 'ADMIN' | 'STAFF';

export type UserStatus = 'ACTIVE' | 'INACTIVE';

/** Optional personal detail; stored as the enum name on the server. */
export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';

/** Send null (or an empty string) to clear an optional personal detail. */
export interface UpdateProfileRequest {
  name: string;
  lastName: string | null;
  gender: Gender | null;
  dateOfBirth: string | null;
  place: string | null;
  address: string | null;
  phone: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Admin-only change of the two identifiers an account can sign in with. */
export interface UpdateLoginIdsRequest {
  email: string;
  staffCode: string;
}

/** Counter "badge" sign-in: the staff code printed on the badge plus a numeric PIN. */
export interface PinLoginRequest {
  staffCode: string;
  pin: string;
}

/** Asks the server to text a one-time code to a mobile number. */
export interface OtpRequestBody {
  phone: string;
}

/** Confirms a code was sent; the code itself is never returned. */
export interface OtpRequestResponse {
  expiresInSeconds: number;
  maskedPhone: string;
}

/** Exchanges a received one-time code for a session. */
export interface OtpVerifyRequest {
  phone: string;
  code: string;
}

/** Carries the ID token returned by the Google or Apple JavaScript SDK. */
export interface SocialLoginRequest {
  idToken: string;
}

export type AuthProviderId = 'password' | 'pin' | 'phone' | 'google' | 'apple';

/** Which sign-in methods the server has actually been configured for. */
export interface AuthProvidersResponse {
  passwordEnabled: boolean;
  pinEnabled: boolean;
  phoneOtpEnabled: boolean;
  googleEnabled: boolean;
  appleEnabled: boolean;
  googleClientId: string | null;
  appleClientId: string | null;
  otpLength: number;
  /** The live OTP channel. `log` means codes go to the backend log, not by SMS. */
  smsProvider: string;
}

/** Response of `POST /api/auth/login`. Never contains the password hash. */
export interface LoginResponse {
  token: string;
  tokenType: string;
  id: number;
  name: string;
  email: string;
  role: Role;
}

/** A user account (as returned by `/api/auth/me` and `/api/users`). */
export interface User {
  id: number;
  name: string;
  lastName: string | null;
  email: string;
  staffCode: string | null;
  phone: string | null;
  gender: Gender | null;
  dateOfBirth: string | null;
  place: string | null;
  address: string | null;
  pinSet: boolean;
  googleLinked: boolean;
  appleLinked: boolean;
  role: Role;
  status: UserStatus;
  createdAt: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** Payload used by an ADMIN to create a STAFF/ADMIN account. */
export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
}
