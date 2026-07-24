package com.apigateway.dto;

public record RouteRequest(
        String pathPrefix,
        String targetUrl,
        int rateLimit,
        boolean requiresAuth
) {}
