package com.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.response.ApiResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple sliding-window rate limiter implemented as a Spring HandlerInterceptor.
 *
 * Strategy:
 *   - Each unique IP address is allowed MAX_REQUESTS within a WINDOW_SECONDS window.
 *   - State is stored in-memory (ConcurrentHashMap). For a multi-node deployment
 *     this should be replaced with a Redis-backed solution (e.g. Bucket4j + Redis).
 *
 * Limits (configurable via constants):
 *   - 100 requests per 60 seconds per IP
 *   - Returns HTTP 429 Too Many Requests with a Retry-After header on violation.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    // â”€â”€â”€ Configuration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static final int    MAX_REQUESTS    = 100;   // max calls per window
    private static final long   WINDOW_SECONDS  = 60L;   // sliding window length

    // â”€â”€â”€ State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static class WindowEntry {
        private final AtomicInteger count;
        private final long windowStart;

        public WindowEntry(AtomicInteger count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }

        public AtomicInteger getCount() { return count; }
        public long getWindowStart() { return windowStart; }
    }

    private final ConcurrentHashMap<String, WindowEntry> requestCounts =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = resolveClientIp(request);
        long   nowEpoch = Instant.now().getEpochSecond();

        WindowEntry entry = requestCounts.compute(clientIp, (ip, existing) -> {
            // No entry yet, or the previous window has expired → start fresh
            if (existing == null || (nowEpoch - existing.getWindowStart()) >= WINDOW_SECONDS) {
                return new WindowEntry(new AtomicInteger(1), nowEpoch);
            }
            existing.getCount().incrementAndGet();
            return existing;
        });

        int currentCount = entry.getCount().get();

        // Set informational headers on every response
        response.setHeader("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(
                Math.max(0, MAX_REQUESTS - currentCount)));

        if (currentCount > MAX_REQUESTS) {
            long retryAfter = WINDOW_SECONDS - (nowEpoch - entry.getWindowStart());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> body = ApiResponse.error(
                    "Rate limit exceeded. You may send " + MAX_REQUESTS +
                    " requests per " + WINDOW_SECONDS + " seconds. " +
                    "Retry after " + retryAfter + " seconds.");

            objectMapper.writeValue(response.getOutputStream(), body);

            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return false;
        }

        return true;
    }

    /**
     * Resolves the real client IP, honouring the X-Forwarded-For header
     * that reverse proxies (nginx, AWS ALB) typically set.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

