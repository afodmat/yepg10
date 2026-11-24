package com.yep.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP sliding-window limit on unauthenticated auth endpoints (brute force / signup abuse).
 * In-memory, so it is per instance; move to Redis or a gateway when running several instances.
 * Uses the servlet remote address. Behind a reverse proxy set FORWARD_HEADERS_STRATEGY=native (or framework) so the real client IP is used;
 * leave it at "none" when the app is directly exposed, otherwise clients could spoof X-Forwarded-For.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final int limit;
    private volatile long lastSweep = System.currentTimeMillis();
    private final java.util.concurrent.atomic.AtomicBoolean sweeping = new java.util.concurrent.atomic.AtomicBoolean(false);
    public RateLimitFilter(@Value("${yep.rate-limit.auth-requests-per-minute}") int limit) { this.limit = limit; }

    @Override protected boolean shouldNotFilter(HttpServletRequest r) {
        String p = r.getRequestURI();
        return !(p.equals("/api/auth/login") || p.equals("/api/auth/register") || p.equals("/api/auth/forgot-password") || p.equals("/api/auth/reset-password"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (now - lastSweep > 60_000 && sweeping.compareAndSet(false, true)) {
            try {
                hits.entrySet().removeIf(e -> { synchronized (e.getValue()) { return e.getValue().isEmpty() || now - e.getValue().peekLast() > 60_000; } });
                lastSweep = now;
            } finally { sweeping.set(false); }
        }
        Deque<Long> q = hits.computeIfAbsent(req.getRemoteAddr(), k -> new ArrayDeque<>());
        boolean blocked;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 60_000) q.pollFirst();
            blocked = q.size() >= limit;
            if (!blocked) q.addLast(now);
        }
        if (blocked) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many attempts. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
