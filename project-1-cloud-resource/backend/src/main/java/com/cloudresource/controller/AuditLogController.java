package com.cloudresource.controller;

import com.cloudresource.dto.AuditLogResponse;
import com.cloudresource.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources/{resourceId}/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getByResourceId(@PathVariable UUID resourceId) {
        return ResponseEntity.ok(auditLogService.findByResourceId(resourceId));
    }
}
