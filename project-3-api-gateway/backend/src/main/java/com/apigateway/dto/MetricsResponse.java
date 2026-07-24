package com.apigateway.dto;

public record MetricsResponse(
        long totalRequests,
        long successRequests,
        long errorRequests,
        double averageLatencyMs,
        long rateLimitedRequests
) {}
