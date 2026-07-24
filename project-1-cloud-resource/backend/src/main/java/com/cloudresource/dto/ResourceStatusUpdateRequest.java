package com.cloudresource.dto;

import com.cloudresource.domain.ResourceStatus;
import jakarta.validation.constraints.NotNull;

public record ResourceStatusUpdateRequest(@NotNull ResourceStatus status) {}
