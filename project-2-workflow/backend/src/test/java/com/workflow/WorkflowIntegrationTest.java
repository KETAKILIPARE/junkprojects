package com.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.domain.SystemRole;
import com.workflow.domain.TaskStatus;
import com.workflow.domain.WorkspaceRole;
import com.workflow.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WorkflowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setup() throws Exception {
        register("alice", SystemRole.SYSTEM_ADMIN);
        register("bob", SystemRole.SYSTEM_MEMBER);
        aliceToken = login("alice");
        bobToken = login("bob");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void register(String username, SystemRole role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest(username, "password123", role))))
                .andExpect(status().isCreated());
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest(username, "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createWorkspace(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private void addMember(String adminToken, String wsId, String username, WorkspaceRole role) throws Exception {
        mockMvc.perform(post("/api/workspaces/" + wsId + "/members")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddMemberRequest(username, role))))
                .andExpect(status().isCreated());
    }

    private String createTask(String token, String wsId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTaskRequest(title, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    void register_shouldReturn409_whenUsernameAlreadyTaken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("alice", "other", SystemRole.SYSTEM_ADMIN))))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturn401_whenWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    // ── Workspace creation RBAC ───────────────────────────────────────────────

    @Test
    void createWorkspace_shouldReturn201_whenSystemAdmin() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"My Team\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Team"));
    }

    @Test
    void createWorkspace_shouldReturn403_whenSystemMember() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bob Workspace\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWorkspace_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"No Auth\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── Workspace visibility ──────────────────────────────────────────────────

    @Test
    void getWorkspaces_shouldOnlyShowWorkspacesUserIsMemberOf() throws Exception {
        createWorkspace(aliceToken, "Alice Team");

        // Alice sees her workspace
        mockMvc.perform(get("/api/workspaces")
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Bob sees nothing
        mockMvc.perform(get("/api/workspaces")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getWorkspaces_shouldShowWorkspace_afterBeingInvited() throws Exception {
        String wsId = createWorkspace(aliceToken, "Alice Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);

        mockMvc.perform(get("/api/workspaces")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── Member management ─────────────────────────────────────────────────────

    @Test
    void addMember_shouldReturn404_whenUserDoesNotExist() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        mockMvc.perform(post("/api/workspaces/" + wsId + "/members")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddMemberRequest("nobody", WorkspaceRole.MEMBER))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addMember_shouldReturn409_whenAlreadyMember() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/" + wsId + "/members")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddMemberRequest("bob", WorkspaceRole.MEMBER))))
                .andExpect(status().isConflict());
    }

    @Test
    void addMember_shouldReturn403_whenCallerIsMemberNotAdmin() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);
        register("charlie", SystemRole.SYSTEM_MEMBER);

        mockMvc.perform(post("/api/workspaces/" + wsId + "/members")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddMemberRequest("charlie", WorkspaceRole.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_shouldReturn204_andMemberLosesAccess() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);

        mockMvc.perform(delete("/api/workspaces/" + wsId + "/members/bob")
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workspaces")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removeMember_shouldReturn403_whenCallerIsMember() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);

        mockMvc.perform(delete("/api/workspaces/" + wsId + "/members/alice")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // ── Task operations ───────────────────────────────────────────────────────

    @Test
    void createTask_shouldReturn201_withTodoStatus_whenMemberCreates() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);

        mockMvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTaskRequest("Fix bug", "Details", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fix bug"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_shouldReturn403_whenNonMemberTriesToCreate() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");

        mockMvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTaskRequest("Hack", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTaskStatus_shouldReturn200_whenMemberUpdates() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        String taskId = createTask(aliceToken, wsId, "My task");

        mockMvc.perform(patch("/api/workspaces/" + wsId + "/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_shouldReturn204_whenAdminDeletes() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        String taskId = createTask(aliceToken, wsId, "Delete me");

        mockMvc.perform(delete("/api/workspaces/" + wsId + "/tasks/" + taskId)
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_shouldReturn403_whenMemberTriesToDelete() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");
        addMember(aliceToken, wsId, "bob", WorkspaceRole.MEMBER);
        String taskId = createTask(aliceToken, wsId, "Protected");

        mockMvc.perform(delete("/api/workspaces/" + wsId + "/tasks/" + taskId)
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTasks_shouldReturn403_whenNonMemberTriesToView() throws Exception {
        String wsId = createWorkspace(aliceToken, "My Team");

        mockMvc.perform(get("/api/workspaces/" + wsId + "/tasks")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }
}
