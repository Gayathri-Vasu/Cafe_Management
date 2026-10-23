/**
 * Development environment settings.
 * The Spring Boot backend runs on http://localhost:8080 by default.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  cafeName: 'Kettle & Kitchen',
  cafeAddress: '123 Coffee Street, Bengaluru, India',
  cafePhone: '+91 98765 43210',
  // Shown when a request never reaches the server. In development that almost
  // always means the backend is not running, so name the expected address.
  unreachableMessage:
    'Cannot reach the server. Please make sure the backend is running on http://localhost:8080.',
};
