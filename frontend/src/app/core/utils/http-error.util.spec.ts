import { HttpErrorResponse } from '@angular/common/http';

import { extractErrorMessage } from './http-error.util';

describe('extractErrorMessage', () => {
  it('prefers the first field validation error returned by the backend', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        message: 'Validation failed',
        fieldErrors: { name: 'Product name is required' },
      },
    });

    expect(extractErrorMessage(error)).toBe('Product name is required');
  });

  it('falls back to the generic message when there are no field errors', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { status: 409, message: 'A bill already exists for this order' },
    });

    expect(extractErrorMessage(error)).toBe('A bill already exists for this order');
  });

  it('explains connection problems when the request never reached the server', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(extractErrorMessage(error)).toContain('Cannot reach the server');
  });

  it('returns the fallback for unknown thrown values', () => {
    expect(extractErrorMessage(null)).toBe('Something went wrong. Please try again.');
    expect(extractErrorMessage('boom', 'Custom fallback')).toBe('Custom fallback');
  });
});
