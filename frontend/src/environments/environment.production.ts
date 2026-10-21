/**
 * Production environment settings.
 * Requests are sent to the same origin under /api so a reverse proxy (nginx,
 * Apache, IIS...) can forward them to the Spring Boot backend.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  cafeName: 'Kettle & Kitchen',
  cafeAddress: '123 Coffee Street, Bengaluru, India',
  cafePhone: '+91 98765 43210',
};
