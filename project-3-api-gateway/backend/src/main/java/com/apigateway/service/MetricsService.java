package com.apigateway.service;

import com.apigateway.dto.MetricsResponse;
import com.apigateway.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final RequestLogRepository requestLogRepository;

    @Transactional(readOnly = true)
    public MetricsResponse getMetrics() {
        Double avgLatency = requestLogRepository.averageLatencyMs();
        return new MetricsResponse(
                requestLogRepository.count(),
                requestLogRepository.countByStatusCodeLessThan(400),
                requestLogRepository.countByStatusCodeGreaterThanEqual(400),
                avgLatency != null ? avgLatency : 0.0,
                requestLogRepository.countByStatusCode(429)
        );
    }
}
