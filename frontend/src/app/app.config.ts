import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';

/**
 * Root configuration.
 *
 * - `provideRouter`      : client-side routing with lazy-loaded feature pages.
 * - `provideHttpClient`  : the `HttpClient` used by every service; the two
 *                          functional interceptors attach the JWT and drive the
 *                          global loading bar.
 * - `provideAnimationsAsync` : animations required by Angular Material.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
    ),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([loadingInterceptor, authInterceptor])),
  ],
};
