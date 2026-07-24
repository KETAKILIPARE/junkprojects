package com.bugreport.repository;

import com.bugreport.domain.Bug;
import com.bugreport.domain.BugSeverity;
import com.bugreport.domain.BugStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugRepository extends JpaRepository<Bug, UUID> {
    List<Bug> findBySeverity(BugSeverity severity);
    List<Bug> findByStatus(BugStatus status);
    List<Bug> findByReportedBy(String reportedBy);
}
