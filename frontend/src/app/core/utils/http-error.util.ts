import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from '../models/api.models';

const DEFAULT_MESSAGE = 'Something went wrong. Please try again.';

/**
 * Turns any thrown value (usually an {@link HttpErrorResponse}) into a message
 * that is safe to show to the user. It understands the backend's `ApiError`
 * shape, plain text bodies and network failures.
 */
export function extractErrorMessage(error: unknown, fallback: string = DEFAULT_MESSAGE): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Please make sure the backend is running on http://localhost:8080.';
    }

    const body: unknown = error.error;

    if (body && typeof body === 'object') {
      const apiError = body as Partial<ApiError>;

      if (apiError.fieldErrors && Object.keys(apiError.fieldErrors).length > 0) {
        const first = Object.values(apiError.fieldErrors)[0];
        if (first) {
          return first;
        }
      }

      if (apiError.message) {
        return apiError.message;
      }
    }

    if (typeof body === 'string' && body.trim().length > 0) {
      return body;
    }

    return error.message || fallback;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

/** Extracts the per-field validation errors returned with a 400 response. */
export function extractFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const apiError = error.error as Partial<ApiError>;
    if (apiError.fieldErrors) {
      return apiError.fieldErrors;
    }
  }
  return {};
}
