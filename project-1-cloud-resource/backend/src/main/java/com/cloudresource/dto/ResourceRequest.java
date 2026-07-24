package com.cloudresource.dto;

import com.cloudresource.domain.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceRequest(
        @NotBlank String name,
        @NotNull ResourceType type,
        @NotBlank String region
) {}
