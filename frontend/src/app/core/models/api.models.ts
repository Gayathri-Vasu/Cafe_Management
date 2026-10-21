/** Standard success envelope returned by the backend for "action only" endpoints. */
export interface ApiMessage {
  message: string;
}

/** Standard error body produced by the backend `GlobalExceptionHandler`. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string> | null;
}
