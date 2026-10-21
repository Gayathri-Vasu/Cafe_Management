import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts signed out', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.isAdmin()).toBe(false);
  });

  it('stores the token and role when an ADMIN logs in', () => {
    service
      .login({ email: 'admin@cafe.com', password: 'Admin@123' })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url.endsWith('/auth/login'));
    expect(request.request.method).toBe('POST');
    request.flush({
      token: 'jwt-token',
      tokenType: 'Bearer',
      id: 1,
      name: 'Cafe Admin',
      email: 'admin@cafe.com',
      role: 'ADMIN',
    });

    expect(service.getToken()).toBe('jwt-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.role()).toBe('ADMIN');
    expect(localStorage.getItem('cafe.token')).toBe('jwt-token');
  });

  it('does not grant admin rights to a STAFF login', () => {
    service.login({ email: 'ravi@cafe.com', password: 'Staff@123' }).subscribe();

    httpMock.expectOne((req) => req.url.endsWith('/auth/login')).flush({
      token: 'staff-token',
      tokenType: 'Bearer',
      id: 2,
      name: 'Ravi Staff',
      email: 'ravi@cafe.com',
      role: 'STAFF',
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.isAdmin()).toBe(false);
    expect(service.isStaff()).toBe(true);
  });

  it('clears the session on logout', () => {
    service.login({ email: 'admin@cafe.com', password: 'Admin@123' }).subscribe();
    httpMock.expectOne((req) => req.url.endsWith('/auth/login')).flush({
      token: 'jwt-token',
      tokenType: 'Bearer',
      id: 1,
      name: 'Cafe Admin',
      email: 'admin@cafe.com',
      role: 'ADMIN',
    });

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
    expect(localStorage.getItem('cafe.token')).toBeNull();
  });
});
