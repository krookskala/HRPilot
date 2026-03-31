package com.hrpilot.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory IP-based rate limiter for sensitive endpoints.
 * Login: max 10 requests per minute.
 * Password reset: max 5 requests per minute.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_LIMIT = 10;
    private static final int PASSWORD_RESET_LIMIT = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateBucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, RateBucket> passwordResetBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);

            if (path.equals("/api/auth/login") || path.equals("/api/auth/refresh")) {
                if (!isAllowed(loginBuckets, clientIp, LOGIN_LIMIT)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":429,\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many login attempts. Please try again later.\"}");
                    return;
                }
            } else if (path.equals("/api/auth/password/request") || path.equals("/api/auth/password/reset")) {
                if (!isAllowed(passwordResetBuckets, clientIp, PASSWORD_RESET_LIMIT)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":429,\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many password reset attempts. Please try again later.\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(Map<String, RateBucket> buckets, String clientIp, int limit) {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS * 2);

        RateBucket bucket = buckets.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateBucket(now, new AtomicInteger(0));
            }
            return existing;
        });

        return bucket.counter.incrementAndGet() <= limit;
    }

    private String getClientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private record RateBucket(long windowStart, AtomicInteger counter) {}
}
