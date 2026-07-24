package com.cloudresource.service;

import com.cloudresource.domain.*;
import com.cloudresource.dto.ResourceRequest;
import com.cloudresource.dto.ResourceResponse;
import com.cloudresource.exception.AccessDeniedException;
import com.cloudresource.exception.InvalidStateTransitionException;
import com.cloudresource.exception.ResourceNotFoundException;
import com.cloudresource.repository.AuditLogRepository;
import com.cloudresource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_STOPPED = "STOPPED";
    private static final String ACTION_TERMINATED = "TERMINATED";
    private static final String ACTION_STATUS_UPDATED = "STATUS_UPDATED";

    private final ResourceRepository resourceRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public ResourceResponse updateStatus(UUID id, ResourceStatus newStatus, String username, UserRole role) {
        if (role == UserRole.VIEWER) {
            throw new AccessDeniedException("Viewers cannot update resource status");
        }
        Resource resource = findResourceById(id);
        resource.setStatus(newStatus);
        resource.setUpdatedAt(Instant.now());
        Resource saved = resourceRepository.save(resource);
        auditLogRepository.save(new AuditLog(id, username, ACTION_STATUS_UPDATED + ":" + newStatus));
        return toResponse(saved);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request, String username, UserRole role) {
        if (role == UserRole.VIEWER) {
            throw new AccessDeniedException("Viewers cannot create resources");
        }
        Resource resource = new Resource(request.name(), request.type(), request.region(), username);
        Resource saved = resourceRepository.save(resource);
        auditLogRepository.save(new AuditLog(saved.getId(), username, ACTION_CREATED));
        return toResponse(saved);
    }

    @Transactional
    public ResourceResponse stop(UUID id, String username, UserRole role) {
        if (role == UserRole.VIEWER) {
            throw new AccessDeniedException("Viewers cannot stop resources");
        }
        Resource resource = findResourceById(id);
        if (resource.getStatus() != ResourceStatus.RUNNING) {
            throw new InvalidStateTransitionException("Only RUNNING resources can be stopped");
        }
        resource.setStatus(ResourceStatus.STOPPED);
        resource.setUpdatedAt(Instant.now());
        Resource saved = resourceRepository.save(resource);
        auditLogRepository.save(new AuditLog(id, username, ACTION_STOPPED));
        return toResponse(saved);
    }

    @Transactional
    public ResourceResponse terminate(UUID id, String username, UserRole role) {
        if (role == UserRole.VIEWER) {
            throw new AccessDeniedException("Viewers cannot terminate resources");
        }
        Resource resource = findResourceById(id);
        if (resource.getStatus() == ResourceStatus.TERMINATED) {
            throw new InvalidStateTransitionException("Resource is already terminated");
        }
        resource.setStatus(ResourceStatus.TERMINATED);
        resource.setUpdatedAt(Instant.now());
        Resource saved = resourceRepository.save(resource);
        auditLogRepository.save(new AuditLog(id, username, ACTION_TERMINATED));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(UUID id) {
        return toResponse(findResourceById(id));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll().stream().map(this::toResponse).toList();
    }

    private Resource findResourceById(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getRegion(),
                resource.getStatus(),
                resource.getCreatedBy(),
                resource.getCreatedAt()
        );
    }
}
