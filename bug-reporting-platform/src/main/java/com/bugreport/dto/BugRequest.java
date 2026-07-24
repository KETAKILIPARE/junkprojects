package com.bugreport.dto;

import jakarta.validation.constraints.NotBlank;

public record BugRequest(@NotBlank String rawDescription) {}
