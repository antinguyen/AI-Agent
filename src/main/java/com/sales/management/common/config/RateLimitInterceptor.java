package com.sales.management.common.config;

import com.sales.management.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding-window rate limiter per IP. Default: 10 attempts per 60 seconds.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String ip = resolveClientIp(request);

        // Skip rate limiting for loopback (e.g. integration tests, internal health checks)
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        Deque<Long> timestamps = requestLog.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        // Remove timestamps outside current window
        timestamps.removeIf(t -> t < windowStart);

        if (timestamps.size() >= maxRequests) {
            throw new TooManyRequestsException(
                    "Too many requests. Limit: " + maxRequests + " per " + windowSeconds + " seconds.");
        }

        timestamps.addLast(now);
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
