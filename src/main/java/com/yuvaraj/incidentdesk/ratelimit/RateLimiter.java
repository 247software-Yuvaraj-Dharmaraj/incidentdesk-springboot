package com.yuvaraj.incidentdesk.ratelimit;

import com.yuvaraj.incidentdesk.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Lightweight in-memory fixed-window rate limiter (mirrors the Node express-rate-limit usage). */
@Component
public class RateLimiter {

    private record Window(long resetAt, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int maxRequests, long windowSeconds) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, current) ->
                (current == null || current.resetAt() <= now)
                        ? new Window(now + windowSeconds * 1000L, new AtomicInteger(0))
                        : current);
        if (window.count().incrementAndGet() > maxRequests) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later.");
        }
    }
}
