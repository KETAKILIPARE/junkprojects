package com.apigateway.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final long DEFAULT_WINDOW_MS = 60_000L;

    private final long windowMs;
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService() {
        this.windowMs = DEFAULT_WINDOW_MS;
    }

    public RateLimiterService(long windowMs) {
        this.windowMs = windowMs;
    }

    public boolean isAllowed(String clientKey, int limit) {
        long now = System.currentTimeMillis();
        ClientBucket bucket = buckets.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStart > windowMs) {
                return new ClientBucket(now);
            }
            return existing;
        });
        return bucket.counter.incrementAndGet() <= limit;
    }

    private static class ClientBucket {
        final long windowStart;
        final AtomicInteger counter = new AtomicInteger(0);

        ClientBucket(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
