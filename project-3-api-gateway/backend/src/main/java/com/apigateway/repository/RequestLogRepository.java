package com.apigateway.repository;

import com.apigateway.domain.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface RequestLogRepository extends JpaRepository<RequestLog, UUID> {
    long countByStatusCodeLessThan(int statusCode);
    long countByStatusCodeGreaterThanEqual(int statusCode);
    long countByStatusCode(int statusCode);

    @Query("SELECT AVG(r.latencyMs) FROM RequestLog r")
    Double averageLatencyMs();
}
