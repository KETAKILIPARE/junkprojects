package com.apigateway.dto;

import java.util.UUID;

public record RouteConfigResponse(
        UUID id,
        String pathPrefix,
        String targetUrl,
        int rateLimit,
        boolean requiresAuth
) {}
