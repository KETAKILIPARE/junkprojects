package com.cloudresource.controller;

import com.cloudresource.domain.ResourceStatus;
import com.cloudresource.domain.ResourceType;
import com.cloudresource.dto.ResourceRequest;
import com.cloudresource.dto.ResourceResponse;
import com.cloudresource.exception.ResourceNotFoundException;
import com.cloudresource.service.ResourceService;
import com.cloudresource.service.UserDetailsServiceImpl;
import com.cloudresource.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
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

@WebMvcTest(value = ResourceController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(com.cloudresource.config.SecurityConfig.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceService resourceService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(resourceService);
    }

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private ResourceResponse sampleResponse() {
        return new ResourceResponse(
                UUID.randomUUID(), "my-server", ResourceType.EC2,
                "us-east-1", ResourceStatus.PENDING, "operator1", Instant.now()
        );
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createResource_shouldReturn201_whenRequestIsValid() throws Exception {
        ResourceRequest request = new ResourceRequest("my-server", ResourceType.EC2, "us-east-1");
        when(resourceService.create(any(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/resources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createResource_shouldReturn403_whenCallerIsViewer() throws Exception {
        ResourceRequest request = new ResourceRequest("my-server", ResourceType.EC2, "us-east-1");

        mockMvc.perform(post("/api/resources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getResource_shouldReturn200_whenResourceExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(resourceService.findById(id)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/resources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("my-server"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getResource_shouldReturn404_whenResourceDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(resourceService.findById(id)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/resources/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getAllResources_shouldReturn200WithList() throws Exception {
        when(resourceService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void terminateResource_shouldReturn200_whenResourceIsRunning() throws Exception {
        UUID id = UUID.randomUUID();
        ResourceResponse terminated = new ResourceResponse(
                id, "my-server", ResourceType.EC2, "us-east-1",
                ResourceStatus.TERMINATED, "admin1", Instant.now()
        );
        when(resourceService.terminate(eq(id), any(), any())).thenReturn(terminated);

        mockMvc.perform(delete("/api/resources/{id}", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    void getAllResources_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }
}
