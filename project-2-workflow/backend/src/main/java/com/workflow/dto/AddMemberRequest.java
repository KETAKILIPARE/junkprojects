package com.workflow.dto;

import com.workflow.domain.WorkspaceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank String username, @NotNull WorkspaceRole role) {}
