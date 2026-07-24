package com.cloudresource.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID resourceId,
        String performedBy,
        String action,
        Instant performedAt
) {}
