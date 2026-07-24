package com.workflow.repository;

import com.workflow.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT w FROM Workspace w WHERE w.id IN (SELECT m.workspaceId FROM WorkspaceMember m WHERE m.username = :username)")
    List<Workspace> findByMemberUsername(@Param("username") String username);
}
