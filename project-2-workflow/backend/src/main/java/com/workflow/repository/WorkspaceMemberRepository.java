package com.workflow.repository;

import com.workflow.domain.WorkspaceMember;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    boolean existsByWorkspaceIdAndUsername(UUID workspaceId, String username);
    Optional<WorkspaceMember> findByWorkspaceIdAndUsername(UUID workspaceId, String username);
    List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

    @Modifying
    @Transactional
    void deleteByWorkspaceIdAndUsername(UUID workspaceId, String username);
}
