package com.cafe.management.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Temporary performance instrumentation.
 *
 * <p>Logs one line per HTTP request with the method, the URI path, the response status
 * and the wall-clock duration. Nothing else is logged: no query string, no headers,
 * no request body, no response body - so credentials, tokens and search terms cannot
 * appear in the logs. Only the path is recorded.
 *
 * <p>Ordered above Spring Security so that the timing covers the whole request,
 * including requests that security or CORS reject before reaching a controller.
 *
 * <p>This class exists only to measure. Delete it (and the file) once the numbers are
 * collected; nothing else depends on it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("REQ_TIMING {} {} -> {} in {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
        }
    }
}
