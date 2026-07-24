package com.bugreport.dto;

import com.bugreport.domain.BugStatus;
import jakarta.validation.constraints.NotNull;

public record BugStatusUpdateRequest(@NotNull BugStatus status) {}
