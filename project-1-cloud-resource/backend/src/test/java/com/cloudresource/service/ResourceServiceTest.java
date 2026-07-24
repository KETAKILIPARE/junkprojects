package com.cloudresource.service;

import com.cloudresource.domain.*;
import com.cloudresource.dto.ResourceRequest;
import com.cloudresource.dto.ResourceResponse;
import com.cloudresource.exception.AccessDeniedException;
import com.cloudresource.exception.InvalidStateTransitionException;
import com.cloudresource.exception.ResourceNotFoundException;
import com.cloudresource.repository.AuditLogRepository;
import com.cloudresource.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ResourceService resourceService;

    private ResourceRequest validRequest;
    private Resource savedResource;

    @BeforeEach
    void setUp() {
        validRequest = new ResourceRequest("my-server", ResourceType.EC2, "us-east-1");
        savedResource = new Resource("my-server", ResourceType.EC2, "us-east-1", "operator1");
    }

    @Test
    void create_shouldReturnPendingStatus_whenRequestIsValid() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        ResourceResponse response = resourceService.create(validRequest, "operator1", UserRole.OPERATOR);

        assertThat(response.status()).isEqualTo(ResourceStatus.PENDING);
    }

    @Test
    void create_shouldPersistResource_whenRequestIsValid() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        resourceService.create(validRequest, "operator1", UserRole.OPERATOR);

        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void create_shouldWriteAuditLog_whenResourceIsCreated() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        resourceService.create(validRequest, "operator1", UserRole.OPERATOR);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void create_shouldThrowAccessDenied_whenCallerIsViewer() {
        assertThatThrownBy(() -> resourceService.create(validRequest, "viewer1", UserRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void terminate_shouldSetStatusToTerminated_whenResourceIsRunning() {
        savedResource.setStatus(ResourceStatus.RUNNING);
        UUID id = savedResource.getId();
        when(resourceRepository.findById(id)).thenReturn(Optional.of(savedResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        ResourceResponse response = resourceService.terminate(id, "admin1", UserRole.ADMIN);

        assertThat(response.status()).isEqualTo(ResourceStatus.TERMINATED);
    }

    @Test
    void terminate_shouldThrowInvalidTransition_whenResourceIsAlreadyTerminated() {
        savedResource.setStatus(ResourceStatus.TERMINATED);
        UUID id = savedResource.getId();
        when(resourceRepository.findById(id)).thenReturn(Optional.of(savedResource));

        assertThatThrownBy(() -> resourceService.terminate(id, "admin1", UserRole.ADMIN))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void terminate_shouldThrowAccessDenied_whenCallerIsViewer() {
        UUID id = savedResource.getId();

        assertThatThrownBy(() -> resourceService.terminate(id, "viewer1", UserRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_shouldReturnResource_whenExists() {
        UUID id = savedResource.getId();
        when(resourceRepository.findById(id)).thenReturn(Optional.of(savedResource));

        ResourceResponse response = resourceService.findById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void findById_shouldThrowResourceNotFound_whenDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldReturnAllResources() {
        when(resourceRepository.findAll()).thenReturn(List.of(savedResource));

        List<ResourceResponse> result = resourceService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void stop_shouldSetStatusToStopped_whenResourceIsRunning() {
        savedResource.setStatus(ResourceStatus.RUNNING);
        UUID id = savedResource.getId();
        when(resourceRepository.findById(id)).thenReturn(Optional.of(savedResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        ResourceResponse response = resourceService.stop(id, "operator1", UserRole.OPERATOR);

        assertThat(response.status()).isEqualTo(ResourceStatus.STOPPED);
    }

    @Test
    void stop_shouldThrowInvalidTransition_whenResourceIsPending() {
        savedResource.setStatus(ResourceStatus.PENDING);
        UUID id = savedResource.getId();
        when(resourceRepository.findById(id)).thenReturn(Optional.of(savedResource));

        assertThatThrownBy(() -> resourceService.stop(id, "operator1", UserRole.OPERATOR))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
