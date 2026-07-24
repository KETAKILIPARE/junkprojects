package com.bugreport.service;

import com.bugreport.domain.Bug;
import com.bugreport.domain.BugSeverity;
import com.bugreport.domain.BugStatus;
import com.bugreport.dto.*;
import com.bugreport.exception.BugNotFoundException;
import com.bugreport.repository.BugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final AiEnhancementService aiEnhancementService;

    @Transactional
    public BugResponse submit(BugRequest request, String reportedBy) {
        Bug bug = new Bug(request.rawDescription(), reportedBy);

        AiEnhancedBug enhanced = aiEnhancementService.enhance(request.rawDescription());
        bug.setStepsToReproduce(enhanced.stepsToReproduce());
        bug.setExpectedBehavior(enhanced.expectedBehavior());
        bug.setActualBehavior(enhanced.actualBehavior());
        bug.setSeverity(enhanced.severity());
        bug.setLabels(enhanced.suggestedLabels());

        return toResponse(bugRepository.save(bug));
    }

    @Transactional
    public BugResponse updateStatus(UUID id, BugStatusUpdateRequest request) {
        Bug bug = findBugById(id);
        bug.setStatus(request.status());
        bug.setUpdatedAt(Instant.now());
        return toResponse(bugRepository.save(bug));
    }

    @Transactional(readOnly = true)
    public BugResponse findById(UUID id) {
        return toResponse(findBugById(id));
    }

    @Transactional(readOnly = true)
    public List<BugResponse> findAll() {
        return bugRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BugResponse> findBySeverity(BugSeverity severity) {
        return bugRepository.findBySeverity(severity).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BugResponse> findByStatus(BugStatus status) {
        return bugRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    private Bug findBugById(UUID id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new BugNotFoundException("Bug not found: " + id));
    }

    private BugResponse toResponse(Bug bug) {
        return new BugResponse(
                bug.getId(), bug.getRawDescription(), bug.getStepsToReproduce(),
                bug.getExpectedBehavior(), bug.getActualBehavior(),
                bug.getSeverity(), bug.getStatus(), bug.getLabels(),
                bug.getAssignee(), bug.getReportedBy(), bug.getReportedAt()
        );
    }
}
