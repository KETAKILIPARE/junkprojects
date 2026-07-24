package com.workflow.dto;

import com.workflow.domain.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        UUID workspaceId,
        String assignee,
        String createdBy,
        Instant createdAt
) {}
