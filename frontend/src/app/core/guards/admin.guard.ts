import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Client-side counterpart of the backend `hasRole('ADMIN')` rules. This is a UX
 * convenience only - the REST API is the real guard.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notification = inject(NotificationService);

  if (auth.isAdmin()) {
    return true;
  }

  notification.error('You do not have permission to open that page.');
  return router.createUrlTree(['/orders']);
};
