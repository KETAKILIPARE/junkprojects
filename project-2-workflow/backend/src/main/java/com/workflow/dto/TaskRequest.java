package com.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskRequest(
        @NotBlank String title,
        String description,
        @NotNull UUID workspaceId,
        String assignee
) {}
