import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { LoadingService } from '../services/loading.service';

/** Keeps the top-of-page progress bar in sync with in-flight requests. */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loading = inject(LoadingService);

  loading.start();

  return next(req).pipe(finalize(() => loading.stop()));
};
