package com.workflow.service;

import com.workflow.domain.*;
import com.workflow.dto.TaskRequest;
import com.workflow.dto.TaskResponse;
import com.workflow.dto.TaskStatusUpdateRequest;
import com.workflow.exception.InvalidTaskTransitionException;
import com.workflow.exception.NotWorkspaceMemberException;
import com.workflow.exception.TaskNotFoundException;
import com.workflow.repository.TaskRepository;
import com.workflow.repository.WorkspaceMemberRepository;
import com.workflow.repository.WorkspaceRepository;
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
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private UUID workspaceId;
    private Task savedTask;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        savedTask = new Task("Fix login bug", "Details here", workspaceId, "member1");
    }

    @Test
    void create_shouldReturnTaskWithTodoStatus_whenMemberIsValid() {
        TaskRequest request = new TaskRequest("Fix login bug", "Details", workspaceId, null);
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(memberRepository.existsByWorkspaceIdAndUsername(workspaceId, "member1")).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.create(request, "member1");

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void create_shouldThrowNotWorkspaceMember_whenUserIsNotMember() {
        TaskRequest request = new TaskRequest("Fix login bug", "Details", workspaceId, null);
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(memberRepository.existsByWorkspaceIdAndUsername(workspaceId, "outsider")).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(request, "outsider"))
                .isInstanceOf(NotWorkspaceMemberException.class);
    }

    @Test
    void updateStatus_shouldTransitionFromTodoToInProgress() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.updateStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), "member1");

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_shouldTransitionFromInProgressToReview() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.updateStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.REVIEW), "member1");

        assertThat(response.status()).isEqualTo(TaskStatus.REVIEW);
    }

    @Test
    void updateStatus_shouldTransitionFromReviewToDone() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.REVIEW);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.updateStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.DONE), "member1");

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateStatus_shouldThrowInvalidTransition_whenSameStatus() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.TODO), "member1"))
                .isInstanceOf(InvalidTaskTransitionException.class);
    }

    @Test
    void updateStatus_shouldBroadcastNotification_whenStatusChanges() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        taskService.updateStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), "member1");

        verify(notificationService).broadcastTaskUpdate(any());
    }

    @Test
    void findByWorkspaceId_shouldReturnAllTasksInWorkspace() {
        when(taskRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(savedTask));

        List<TaskResponse> result = taskService.findByWorkspaceId(workspaceId);

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_shouldThrowTaskNotFound_whenDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(id))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
