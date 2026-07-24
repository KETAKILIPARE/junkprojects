package com.bugreport.dto;

import com.bugreport.domain.BugSeverity;
import com.bugreport.domain.BugStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BugResponse(
        UUID id,
        String rawDescription,
        String stepsToReproduce,
        String expectedBehavior,
        String actualBehavior,
        BugSeverity severity,
        BugStatus status,
        List<String> labels,
        String assignee,
        String reportedBy,
        Instant reportedAt
) {}
