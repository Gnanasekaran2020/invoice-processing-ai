package com.invoice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String key = ip + ":" + bucketCategory(request.getRequestURI());
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(request.getRequestURI()));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please slow down.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}"
            );
        }
    }

    private Bucket createBucket(String path) {
        if (path.contains("/auth/")) {
            // 10 auth attempts per minute per IP
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                    .build();
        }
        if (path.contains("/invoices/upload")) {
            // 30 uploads per hour per IP
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofHours(1))))
                    .build();
        }
        // General API: 100 requests per minute per IP
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }

    private String bucketCategory(String path) {
        if (path.contains("/auth/")) return "auth";
        if (path.contains("/invoices/upload")) return "upload";
        return "general";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
