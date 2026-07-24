package com.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private UUID workspaceId;

    private String assignee;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Task(String title, String description, UUID workspaceId, String createdBy) {
        this.title = title;
        this.description = description;
        this.workspaceId = workspaceId;
        this.createdBy = createdBy;
        this.status = TaskStatus.TODO;
        this.createdAt = Instant.now();
    }
}
