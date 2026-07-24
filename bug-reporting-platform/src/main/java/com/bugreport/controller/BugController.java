package com.bugreport.controller;

import com.bugreport.domain.BugSeverity;
import com.bugreport.domain.BugStatus;
import com.bugreport.dto.BugRequest;
import com.bugreport.dto.BugResponse;
import com.bugreport.dto.BugStatusUpdateRequest;
import com.bugreport.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugService bugService;

    @PostMapping
    public ResponseEntity<BugResponse> submit(
            @Valid @RequestBody BugRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bugService.submit(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<BugResponse>> getAll(
            @RequestParam(required = false) BugSeverity severity,
            @RequestParam(required = false) BugStatus status) {
        if (severity != null) return ResponseEntity.ok(bugService.findBySeverity(severity));
        if (status != null) return ResponseEntity.ok(bugService.findByStatus(status));
        return ResponseEntity.ok(bugService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BugResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(bugService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BugResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BugStatusUpdateRequest request) {
        return ResponseEntity.ok(bugService.updateStatus(id, request));
    }
}
