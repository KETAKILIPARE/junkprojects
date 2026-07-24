package com.bugreport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bugs")
@Getter
@Setter
@NoArgsConstructor
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawDescription;

    @Column(columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(columnDefinition = "TEXT")
    private String actualBehavior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugStatus status;

    @ElementCollection
    @CollectionTable(name = "bug_labels", joinColumns = @JoinColumn(name = "bug_id"))
    @Column(name = "label")
    private List<String> labels;

    private String assignee;

    @Column(nullable = false)
    private String reportedBy;

    @Column(nullable = false)
    private Instant reportedAt;

    private Instant updatedAt;

    public Bug(String rawDescription, String reportedBy) {
        this.rawDescription = rawDescription;
        this.reportedBy = reportedBy;
        this.status = BugStatus.OPEN;
        this.severity = BugSeverity.MEDIUM;
        this.reportedAt = Instant.now();
    }
}
