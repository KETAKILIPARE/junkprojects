package com.workflow.controller;

import com.workflow.domain.TaskStatus;
import com.workflow.dto.TaskRequest;
import com.workflow.dto.TaskResponse;
import com.workflow.dto.TaskStatusUpdateRequest;
import com.workflow.exception.TaskNotFoundException;
import com.workflow.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.workflow.util.JwtUtil;
import com.workflow.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TaskController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private TaskResponse sampleResponse(TaskStatus status) {
        return new TaskResponse(UUID.randomUUID(), "Fix login bug", "Details",
                status, UUID.randomUUID(), null, "member1", Instant.now());
    }

    @Test
    @WithMockUser
    void createTask_shouldReturn201_whenRequestIsValid() throws Exception {
        TaskRequest request = new TaskRequest("Fix login bug", "Details", UUID.randomUUID(), null);
        when(taskService.create(any(), any())).thenReturn(sampleResponse(TaskStatus.TODO));

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @WithMockUser
    void updateStatus_shouldReturn200_whenTransitionIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        TaskStatusUpdateRequest update = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);
        when(taskService.updateStatus(eq(id), any(), any())).thenReturn(sampleResponse(TaskStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/tasks/{id}/status", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser
    void getTask_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.findById(id)).thenThrow(new TaskNotFoundException("Not found"));

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getTasksByWorkspace_shouldReturn200WithList() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(taskService.findByWorkspaceId(workspaceId)).thenReturn(List.of(sampleResponse(TaskStatus.TODO)));

        mockMvc.perform(get("/api/tasks").param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createTask_shouldReturn401_whenNotAuthenticated() throws Exception {
        TaskRequest request = new TaskRequest("Fix login bug", "Details", UUID.randomUUID(), null);

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
