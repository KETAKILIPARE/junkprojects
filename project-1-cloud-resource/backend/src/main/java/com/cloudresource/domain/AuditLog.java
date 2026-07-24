package com.cloudresource.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private Instant performedAt;

    public AuditLog(UUID resourceId, String performedBy, String action) {
        this.resourceId = resourceId;
        this.performedBy = performedBy;
        this.action = action;
        this.performedAt = Instant.now();
    }
}
