package com.workflow.dto;

import com.workflow.domain.TaskStatus;

import java.util.UUID;

public record TaskNotification(
        UUID taskId,
        String title,
        TaskStatus status,
        UUID workspaceId,
        String updatedBy
) {}
