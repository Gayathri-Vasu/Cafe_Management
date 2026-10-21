import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Attaches the JWT to every outgoing request and signs the user out when the
 * backend answers `401 Unauthorized` (expired or revoked token).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notification = inject(NotificationService);

  const isLoginRequest = req.url.includes('/auth/login');
  const token = auth.getToken();

  const authorizedRequest =
    token && !isLoginRequest
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isLoginRequest && auth.isAuthenticated()) {
        auth.logout();
        notification.error('Your session has expired. Please sign in again.');
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
