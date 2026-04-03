package com.sales.management.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits POST /api/v1/auth/login to 5 requests per minute per client IP.
 * Uses a simple fixed window counter — no external dependency required.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private record WindowEntry(AtomicInteger count, long windowStartMs) {}

    private final Map<String, WindowEntry> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        long now = System.currentTimeMillis();

        WindowEntry entry = windows.compute(ip, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs()) >= WINDOW_MS) {
                return new WindowEntry(new AtomicInteger(0), now);
            }
            return existing;
        });

        int requests = entry.count().incrementAndGet();

        if (requests <= MAX_REQUESTS) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"code":"TOO_MANY_REQUESTS","message":"Rate limit exceeded. Try again in 1 minute.","details":{},"path":"%s","timestamp":"%s"}
                    """.formatted(request.getRequestURI(), Instant.now()));
        }
    }

    /** Returns X-Forwarded-For header if present (for proxies), otherwise REMOTE_ADDR. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
