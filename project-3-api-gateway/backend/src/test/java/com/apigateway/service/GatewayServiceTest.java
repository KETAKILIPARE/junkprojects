package com.apigateway.service;

import com.apigateway.domain.RouteConfig;
import com.apigateway.exception.RateLimitExceededException;
import com.apigateway.exception.RouteNotFoundException;
import com.apigateway.repository.RequestLogRepository;
import com.apigateway.repository.RouteConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private RouteConfigRepository routeConfigRepository;

    @Mock
    private RequestLogRepository requestLogRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private GatewayService gatewayService;

    @Test
    void route_shouldThrowRouteNotFound_whenNoMatchingRoute() {
        when(routeConfigRepository.findByPathPrefixStartingWith("/unknown/resource")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gatewayService.resolveRoute("/unknown/resource", "127.0.0.1"))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void route_shouldThrowRateLimitExceeded_whenClientExceedsLimit() {
        RouteConfig route = new RouteConfig("/api/users", "http://user-service:8081", 10, false);
        when(routeConfigRepository.findByPathPrefixStartingWith("/api/users")).thenReturn(Optional.of(route));
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> gatewayService.resolveRoute("/api/users/123", "192.168.1.1"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void route_shouldReturnRouteConfig_whenRouteExistsAndRateLimitNotExceeded() {
        RouteConfig route = new RouteConfig("/api/users", "http://user-service:8081", 10, false);
        when(routeConfigRepository.findByPathPrefixStartingWith("/api/users")).thenReturn(Optional.of(route));
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);

        RouteConfig result = gatewayService.resolveRoute("/api/users/123", "192.168.1.1");

        assertThat(result.getTargetUrl()).isEqualTo("http://user-service:8081");
    }

    @Test
    void route_shouldLogRequest_afterResolvingRoute() {
        RouteConfig route = new RouteConfig("/api/users", "http://user-service:8081", 10, false);
        when(routeConfigRepository.findByPathPrefixStartingWith("/api/users")).thenReturn(Optional.of(route));
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);

        gatewayService.resolveRoute("/api/users/123", "192.168.1.1");

        verify(requestLogRepository).save(any());
    }
}
