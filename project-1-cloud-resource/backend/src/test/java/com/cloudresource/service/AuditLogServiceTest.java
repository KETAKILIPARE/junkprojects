package com.cloudresource.service;

import com.cloudresource.domain.AuditLog;
import com.cloudresource.dto.AuditLogResponse;
import com.cloudresource.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void findByResourceId_shouldReturnLogs_whenLogsExist() {
        UUID resourceId = UUID.randomUUID();
        AuditLog log = new AuditLog(resourceId, "admin1", "CREATED");
        when(auditLogRepository.findByResourceId(resourceId)).thenReturn(List.of(log));

        List<AuditLogResponse> result = auditLogService.findByResourceId(resourceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).action()).isEqualTo("CREATED");
    }

    @Test
    void findByResourceId_shouldReturnEmpty_whenNoLogsExist() {
        UUID resourceId = UUID.randomUUID();
        when(auditLogRepository.findByResourceId(resourceId)).thenReturn(List.of());

        List<AuditLogResponse> result = auditLogService.findByResourceId(resourceId);

        assertThat(result).isEmpty();
    }
}
