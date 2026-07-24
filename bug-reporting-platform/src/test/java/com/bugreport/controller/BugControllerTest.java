package com.bugreport.controller;

import com.bugreport.domain.BugSeverity;
import com.bugreport.domain.BugStatus;
import com.bugreport.dto.BugRequest;
import com.bugreport.dto.BugResponse;
import com.bugreport.dto.BugStatusUpdateRequest;
import com.bugreport.exception.BugNotFoundException;
import com.bugreport.service.BugService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(BugController.class)
class BugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BugService bugService;

    private BugResponse sampleResponse(BugStatus status) {
        return new BugResponse(
                UUID.randomUUID(), "raw description", "steps", "expected",
                "actual", BugSeverity.MEDIUM, status,
                List.of("frontend"), null, "reporter1", Instant.now()
        );
    }

    @Test
    @WithMockUser
    void submitBug_shouldReturn201_whenRequestIsValid() throws Exception {
        BugRequest request = new BugRequest("When I click save nothing happens");
        when(bugService.submit(any(), any())).thenReturn(sampleResponse(BugStatus.OPEN));

        mockMvc.perform(post("/api/bugs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    void submitBug_shouldReturn400_whenDescriptionIsBlank() throws Exception {
        BugRequest request = new BugRequest("");

        mockMvc.perform(post("/api/bugs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getBug_shouldReturn200_whenBugExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(bugService.findById(id)).thenReturn(sampleResponse(BugStatus.OPEN));

        mockMvc.perform(get("/api/bugs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("MEDIUM"));
    }

    @Test
    @WithMockUser
    void getBug_shouldReturn404_whenBugDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(bugService.findById(id)).thenThrow(new BugNotFoundException("Not found"));

        mockMvc.perform(get("/api/bugs/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateStatus_shouldReturn200_whenTransitionIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        BugStatusUpdateRequest update = new BugStatusUpdateRequest(BugStatus.IN_PROGRESS);
        when(bugService.updateStatus(eq(id), any())).thenReturn(sampleResponse(BugStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/bugs/{id}/status", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser
    void getAllBugs_shouldReturn200WithList() throws Exception {
        when(bugService.findAll()).thenReturn(List.of(sampleResponse(BugStatus.OPEN)));

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void submitBug_shouldReturn401_whenNotAuthenticated() throws Exception {
        BugRequest request = new BugRequest("some description");

        mockMvc.perform(post("/api/bugs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
