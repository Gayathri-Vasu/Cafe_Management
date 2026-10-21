import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiMessage } from '../models/api.models';
import {
  AuthProvidersResponse,
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  OtpRequestBody,
  OtpRequestResponse,
  OtpVerifyRequest,
  PinLoginRequest,
  Role,
  SocialLoginRequest,
  UpdateLoginIdsRequest,
  UpdateProfileRequest,
  User,
} from '../models/auth.models';

const TOKEN_KEY = 'cafe.token';
const USER_KEY = 'cafe.user';

/**
 * Single source of truth for authentication state.
 *
 * The JWT and a lightweight copy of the user are persisted in `localStorage` so
 * the guards can decide synchronously (without an extra round trip) whether a
 * route may be activated. The authoritative user record is refreshed from
 * `GET /api/auth/me`.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  private readonly tokenState = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly userState = signal<User | null>(readUser());

  readonly user = this.userState.asReadonly();
  readonly role = computed<Role | null>(() => this.userState()?.role ?? null);
  readonly isAdmin = computed(() => this.userState()?.role === 'ADMIN');
  readonly isStaff = computed(() => this.userState()?.role === 'STAFF');
  readonly isLoggedIn = computed(() => this.tokenState() !== null);

  getToken(): string | null {
    return this.tokenState();
  }

  isAuthenticated(): boolean {
    return this.tokenState() !== null;
  }

  hasRole(role: Role): boolean {
    return this.userState()?.role === role;
  }

  /** `POST /api/auth/login` - stores the JWT and the user on success. */
  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/login`, payload)
      .pipe(tap((response) => this.establishSession(response)));
  }

  /**
   * `POST /api/auth/pin-login` - counter sign-in with the staff code printed on
   * the badge plus a numeric PIN.
   */
  loginWithPin(payload: PinLoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/pin-login`, payload)
      .pipe(tap((response) => this.establishSession(response)));
  }

  /** `POST /api/auth/otp/request` - texts a one-time code to a linked number. */
  requestOtp(payload: OtpRequestBody): Observable<OtpRequestResponse> {
    return this.http.post<OtpRequestResponse>(`${this.baseUrl}/otp/request`, payload);
  }

  /** `POST /api/auth/otp/verify` - exchanges that code for a session. */
  verifyOtp(payload: OtpVerifyRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/otp/verify`, payload)
      .pipe(tap((response) => this.establishSession(response)));
  }

  /**
   * `POST /api/auth/google` - verifies the ID token minted by Google Identity
   * Services. The token is checked against Google's public keys on the server.
   */
  loginWithGoogle(payload: SocialLoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/google`, payload)
      .pipe(tap((response) => this.establishSession(response)));
  }

  /** `POST /api/auth/apple` - the Apple equivalent of {@link loginWithGoogle}. */
  loginWithApple(payload: SocialLoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/apple`, payload)
      .pipe(tap((response) => this.establishSession(response)));
  }

  /**
   * `GET /api/auth/providers` - tells the login screen which methods the server
   * can actually complete, so a button is never offered for a disabled flow.
   */
  loadProviders(): Observable<AuthProvidersResponse> {
    return this.http.get<AuthProvidersResponse>(`${this.baseUrl}/providers`);
  }

  /** `GET /api/auth/me` - refreshes the cached profile from the database. */
  loadCurrentUser(): Observable<User> {
    return this.http
      .get<User>(`${this.baseUrl}/me`)
      .pipe(tap((user) => this.setUser(user)));
  }

  /** `POST /api/auth/change-password` */
  changePassword(payload: ChangePasswordRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${this.baseUrl}/change-password`, payload);
  }

  /** `PUT /api/auth/profile` - saves the personal details and returns the updated profile. */
  updateProfile(payload: UpdateProfileRequest): Observable<User> {
    return this.http
      .put<User>(`${this.baseUrl}/profile`, payload)
      .pipe(tap((user) => this.setUser(user)));
  }

  /**
   * `PUT /api/auth/login-ids` - admin-only change of the mail ID and staff code.
   *
   * The server signs a fresh token against the new email, so the old one is
   * replaced before the profile is re-read.
   */
  updateLoginIds(payload: UpdateLoginIdsRequest): Observable<User> {
    return this.http.put<LoginResponse>(`${this.baseUrl}/login-ids`, payload).pipe(
      tap((response) => this.setToken(response.token)),
      switchMap(() => this.loadCurrentUser()),
    );
  }

  /**
   * Clears the session. Tokens are stateless on the server, so logging out is
   * simply "forget the token on the client".
   */
  logout(): void {
    this.setToken(null);
    this.setUser(null);
  }

  /**
   * Every sign-in route returns the same {@link LoginResponse}, so they all end
   * here. The extra profile fields are filled in by the first `GET /auth/me`.
   */
  private establishSession(response: LoginResponse): void {
    this.setToken(response.token);
    this.setUser({
      id: response.id,
      name: response.name,
      lastName: null,
      email: response.email,
      staffCode: null,
      phone: null,
      gender: null,
      dateOfBirth: null,
      place: null,
      address: null,
      pinSet: false,
      googleLinked: false,
      appleLinked: false,
      role: response.role,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    });
  }

  private setToken(token: string | null): void {
    this.tokenState.set(token);
    writeStorage(TOKEN_KEY, token);
  }

  private setUser(user: User | null): void {
    this.userState.set(user);
    if (user) {
      writeStorage(USER_KEY, JSON.stringify(user));
    } else {
      removeStorage(USER_KEY);
    }
  }
}

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string | null): void {
  try {
    if (value === null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  } catch {
    /* storage unavailable - the session simply will not survive a reload */
  }
}

function removeStorage(key: string): void {
  writeStorage(key, null);
}

function readUser(): User | null {
  const raw = readStorage(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}
