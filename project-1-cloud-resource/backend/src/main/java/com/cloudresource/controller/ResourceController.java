package com.cloudresource.controller;

import com.cloudresource.domain.UserRole;
import com.cloudresource.dto.ResourceRequest;
import com.cloudresource.dto.ResourceResponse;
import com.cloudresource.dto.ResourceStatusUpdateRequest;
import com.cloudresource.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRole role = extractRole(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(request, userDetails.getUsername(), role));
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ResourceResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ResourceStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRole role = extractRole(userDetails);
        return ResponseEntity.ok(resourceService.updateStatus(id, request.status(), userDetails.getUsername(), role));
    }

    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ResourceResponse> stop(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRole role = extractRole(userDetails);
        return ResponseEntity.ok(resourceService.stop(id, userDetails.getUsername(), role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ResourceResponse> terminate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRole role = extractRole(userDetails);
        return ResponseEntity.ok(resourceService.terminate(id, userDetails.getUsername(), role));
    }

    private UserRole extractRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElseThrow();
    }
}
