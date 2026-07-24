package com.apigateway.service;

import com.apigateway.domain.RequestLog;
import com.apigateway.domain.RouteConfig;
import com.apigateway.exception.RateLimitExceededException;
import com.apigateway.exception.RouteNotFoundException;
import com.apigateway.repository.RequestLogRepository;
import com.apigateway.repository.RouteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GatewayService {

    private final RouteConfigRepository routeConfigRepository;
    private final RequestLogRepository requestLogRepository;
    private final RateLimiterService rateLimiterService;

    public RouteConfig resolveRoute(String path, String clientIp) {
        String prefix = extractPrefix(path);
        RouteConfig route = routeConfigRepository.findByPathPrefixStartingWith(prefix)
                .orElseThrow(() -> new RouteNotFoundException("No route found for path: " + path));

        if (!rateLimiterService.isAllowed(clientIp, route.getRateLimit())) {
            requestLogRepository.save(new RequestLog("UNKNOWN", path, clientIp, 429, 0));
            throw new RateLimitExceededException("Rate limit exceeded for client: " + clientIp);
        }

        requestLogRepository.save(new RequestLog("UNKNOWN", path, clientIp, 200, 0));
        return route;
    }

    private String extractPrefix(String path) {
        String[] parts = path.split("/");
        return parts.length >= 3 ? "/" + parts[1] + "/" + parts[2] : path;
    }
}
