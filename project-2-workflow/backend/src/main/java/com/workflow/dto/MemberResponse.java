package com.workflow.dto;

import com.workflow.domain.WorkspaceRole;

public record MemberResponse(String username, WorkspaceRole role) {}
