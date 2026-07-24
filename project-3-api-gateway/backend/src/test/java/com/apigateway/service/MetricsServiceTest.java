package com.apigateway.service;

import com.apigateway.dto.MetricsResponse;
import com.apigateway.repository.RequestLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @InjectMocks
    private MetricsService metricsService;

    @Test
    void getMetrics_shouldReturnCorrectTotals() {
        when(requestLogRepository.count()).thenReturn(100L);
        when(requestLogRepository.countByStatusCodeLessThan(400)).thenReturn(90L);
        when(requestLogRepository.countByStatusCodeGreaterThanEqual(400)).thenReturn(10L);
        when(requestLogRepository.averageLatencyMs()).thenReturn(45.5);
        when(requestLogRepository.countByStatusCode(429)).thenReturn(5L);

        MetricsResponse metrics = metricsService.getMetrics();

        assertThat(metrics.totalRequests()).isEqualTo(100L);
        assertThat(metrics.successRequests()).isEqualTo(90L);
        assertThat(metrics.errorRequests()).isEqualTo(10L);
        assertThat(metrics.averageLatencyMs()).isEqualTo(45.5);
        assertThat(metrics.rateLimitedRequests()).isEqualTo(5L);
    }
}
