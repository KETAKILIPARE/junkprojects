package com.bugreport.dto;

import com.bugreport.domain.BugSeverity;

import java.util.List;

public record AiEnhancedBug(
        String stepsToReproduce,
        String expectedBehavior,
        String actualBehavior,
        BugSeverity severity,
        List<String> suggestedLabels
) {}
