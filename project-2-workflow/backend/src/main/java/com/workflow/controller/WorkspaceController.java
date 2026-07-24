package com.workflow.controller;

import com.workflow.domain.Workspace;
import com.workflow.domain.WorkspaceMember;
import com.workflow.domain.WorkspaceRole;
import com.workflow.dto.*;
import com.workflow.exception.InsufficientRoleException;
import com.workflow.exception.NotWorkspaceMemberException;
import com.workflow.repository.UserRepository;
import com.workflow.repository.WorkspaceMemberRepository;
import com.workflow.repository.WorkspaceRepository;
import com.workflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final TaskService taskService;
    private final UserRepository userRepository;

    // ── Workspace CRUD ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Workspace> create(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isSystemAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        if (!isSystemAdmin) {
            throw new InsufficientRoleException("Only SYSTEM_ADMINs can create workspaces");
        }
        Workspace workspace = workspaceRepository.save(
                new Workspace(body.get("name"), userDetails.getUsername()));
        memberRepository.save(
                new WorkspaceMember(workspace.getId(), userDetails.getUsername(), WorkspaceRole.ADMIN));
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace);
    }

    @GetMapping
    public ResponseEntity<List<Workspace>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                workspaceRepository.findByMemberUsername(userDetails.getUsername()));
    }

    // ── Member management (ADMIN only) ──────────────────────────────────────

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireMember(workspaceId, userDetails.getUsername());
        List<MemberResponse> members = memberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(m -> new MemberResponse(m.getUsername(), m.getRole()))
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireRole(workspaceId, userDetails.getUsername(), WorkspaceRole.ADMIN);
        if (!userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User '" + request.username() + "' does not exist");
        }
        if (memberRepository.findByWorkspaceIdAndUsername(workspaceId, request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User '" + request.username() + "' is already a member");
        }
        WorkspaceMember member = memberRepository.save(
                new WorkspaceMember(workspaceId, request.username(), request.role()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MemberResponse(member.getUsername(), member.getRole()));
    }

    @DeleteMapping("/{workspaceId}/members/{username}")
    @Transactional
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireRole(workspaceId, userDetails.getUsername(), WorkspaceRole.ADMIN);
        memberRepository.deleteByWorkspaceIdAndUsername(workspaceId, username);
        return ResponseEntity.noContent().build();
    }

    // ── Task operations ──────────────────────────────────────────────────────

    @PostMapping("/{workspaceId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireMember(workspaceId, userDetails.getUsername());
        TaskRequest taskRequest = new TaskRequest(
                request.title(), request.description(), workspaceId, request.assignee());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(taskRequest, userDetails.getUsername()));
    }

    @GetMapping("/{workspaceId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireMember(workspaceId, userDetails.getUsername());
        return ResponseEntity.ok(taskService.findByWorkspaceId(workspaceId));
    }

    @PatchMapping("/{workspaceId}/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireMember(workspaceId, userDetails.getUsername());
        return ResponseEntity.ok(taskService.updateStatus(taskId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireRole(workspaceId, userDetails.getUsername(), WorkspaceRole.ADMIN);
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    // ── RBAC helpers ─────────────────────────────────────────────────────────

    private WorkspaceMember requireMember(UUID workspaceId, String username) {
        return memberRepository.findByWorkspaceIdAndUsername(workspaceId, username)
                .orElseThrow(() -> new NotWorkspaceMemberException(
                        "You are not a member of this workspace"));
    }

    private void requireRole(UUID workspaceId, String username, WorkspaceRole required) {
        WorkspaceMember member = requireMember(workspaceId, username);
        if (member.getRole() != required) {
            throw new InsufficientRoleException(
                    "This action requires " + required + " role");
        }
    }
}
