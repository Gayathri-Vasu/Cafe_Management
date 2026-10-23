/**
 * Production environment settings.
 * Requests go straight to the deployed Spring Boot backend on Render, which is a
 * different origin from the site, so the backend must allow this origin via CORS.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://cafe-management-w7d5.onrender.com/api',
  cafeName: 'Kettle & Kitchen',
  cafeAddress: '123 Coffee Street, Bengaluru, India',
  cafePhone: '+91 98765 43210',
  // Shown when a request never reaches the server. In production the usual cause
  // is a dropped connection or a blocked CORS preflight, so do not name localhost.
  unreachableMessage: 'Cannot reach the server. Please check your connection and try again.',
};
