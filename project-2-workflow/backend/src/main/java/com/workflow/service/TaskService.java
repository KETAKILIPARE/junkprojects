package com.workflow.service;

import com.workflow.domain.Task;
import com.workflow.domain.TaskStatus;
import com.workflow.dto.*;
import com.workflow.exception.InvalidTaskTransitionException;
import com.workflow.exception.NotWorkspaceMemberException;
import com.workflow.exception.TaskNotFoundException;
import com.workflow.repository.TaskRepository;
import com.workflow.repository.WorkspaceMemberRepository;
import com.workflow.repository.WorkspaceRepository;
import com.workflow.util.TaskStateTransitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse create(TaskRequest request, String username) {
        if (!workspaceRepository.existsById(request.workspaceId())) {
            throw new com.workflow.exception.WorkspaceNotFoundException("Workspace not found");
        }
        if (!memberRepository.existsByWorkspaceIdAndUsername(request.workspaceId(), username)) {
            throw new NotWorkspaceMemberException("User is not a member of this workspace");
        }
        Task task = new Task(request.title(), request.description(), request.workspaceId(), username);
        if (request.assignee() != null) {
            task.setAssignee(request.assignee());
        }
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(UUID taskId, TaskStatusUpdateRequest request, String username) {
        Task task = findTaskById(taskId);
        TaskStatus newStatus = request.status();

        if (!TaskStateTransitionValidator.isValid(task.getStatus(), newStatus)) {
            throw new InvalidTaskTransitionException(
                    "Cannot transition from " + task.getStatus() + " to " + newStatus);
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(Instant.now());
        Task saved = taskRepository.save(task);

        notificationService.broadcastTaskUpdate(new TaskNotification(
                saved.getId(), saved.getTitle(), saved.getStatus(), saved.getWorkspaceId(), username));

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        return toResponse(findTaskById(id));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findByWorkspaceId(UUID workspaceId) {
        return taskRepository.findByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    private Task findTaskById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getWorkspaceId(),
                task.getAssignee(), task.getCreatedBy(), task.getCreatedAt()
        );
    }
}
