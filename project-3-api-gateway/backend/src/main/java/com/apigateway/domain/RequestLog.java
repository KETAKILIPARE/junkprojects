package com.apigateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_logs")
@Getter
@NoArgsConstructor
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String clientIp;

    @Column(nullable = false)
    private int statusCode;

    @Column(nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private Instant requestedAt;

    public RequestLog(String method, String path, String clientIp, int statusCode, long latencyMs) {
        this.method = method;
        this.path = path;
        this.clientIp = clientIp;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.requestedAt = Instant.now();
    }
}
