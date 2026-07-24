package com.apigateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenUnderLimit() {
        boolean result = rateLimiterService.isAllowed("client-1", 5);

        assertThat(result).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalse_whenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("client-2", 3);
        }

        boolean result = rateLimiterService.isAllowed("client-2", 3);

        assertThat(result).isFalse();
    }

    @Test
    void isAllowed_shouldTrackClientsIndependently() {
        for (int i = 0; i < 3; i++) {
            rateLimiterService.isAllowed("client-3", 3);
        }

        boolean otherClient = rateLimiterService.isAllowed("client-4", 3);

        assertThat(otherClient).isTrue();
    }

    @Test
    void isAllowed_shouldAllowRequestsAfterWindowResets() throws InterruptedException {
        RateLimiterService shortWindowService = new RateLimiterService(100);
        shortWindowService.isAllowed("client-5", 1);
        shortWindowService.isAllowed("client-5", 1);

        Thread.sleep(150);

        boolean result = shortWindowService.isAllowed("client-5", 1);

        assertThat(result).isTrue();
    }
}
