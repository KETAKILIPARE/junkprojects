package com.cloudresource.service;

import com.cloudresource.dto.AuditLogResponse;
import com.cloudresource.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByResourceId(UUID resourceId) {
        return auditLogRepository.findByResourceId(resourceId).stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getResourceId(),
                        log.getPerformedBy(),
                        log.getAction(),
                        log.getPerformedAt()
                ))
                .toList();
    }
}
