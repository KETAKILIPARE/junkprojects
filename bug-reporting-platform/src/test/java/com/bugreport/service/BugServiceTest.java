package com.bugreport.service;

import com.bugreport.domain.*;
import com.bugreport.dto.*;
import com.bugreport.exception.BugNotFoundException;
import com.bugreport.repository.BugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugServiceTest {

    @Mock
    private BugRepository bugRepository;

    @Mock
    private AiEnhancementService aiEnhancementService;

    @InjectMocks
    private BugService bugService;

    private Bug savedBug;

    @BeforeEach
    void setUp() {
        savedBug = new Bug("When I click save nothing happens", "reporter1");
    }

    @Test
    void submit_shouldReturnBugWithOpenStatus_whenRequestIsValid() {
        AiEnhancedBug aiResult = new AiEnhancedBug(
                "1. Go to profile\n2. Click save",
                "Email should update",
                "Email stays the same",
                BugSeverity.MEDIUM,
                List.of("frontend", "profile")
        );
        when(aiEnhancementService.enhance(any())).thenReturn(aiResult);
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);

        BugResponse response = bugService.submit(new BugRequest("When I click save nothing happens"), "reporter1");

        assertThat(response.status()).isEqualTo(BugStatus.OPEN);
    }

    @Test
    void submit_shouldApplyAiEnhancement_whenBugIsSubmitted() {
        AiEnhancedBug aiResult = new AiEnhancedBug(
                "Steps", "Expected", "Actual", BugSeverity.HIGH, List.of("backend")
        );
        when(aiEnhancementService.enhance(any())).thenReturn(aiResult);
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);

        bugService.submit(new BugRequest("raw description"), "reporter1");

        verify(aiEnhancementService).enhance("raw description");
    }

    @Test
    void submit_shouldPersistBugWithAiFields_whenEnhancementSucceeds() {
        AiEnhancedBug aiResult = new AiEnhancedBug(
                "Steps", "Expected", "Actual", BugSeverity.CRITICAL, List.of("auth")
        );
        when(aiEnhancementService.enhance(any())).thenReturn(aiResult);
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);

        bugService.submit(new BugRequest("raw description"), "reporter1");

        verify(bugRepository).save(argThat(bug ->
                bug.getSeverity() == BugSeverity.CRITICAL &&
                bug.getStepsToReproduce().equals("Steps")
        ));
    }

    @Test
    void updateStatus_shouldTransitionFromOpenToInProgress() {
        savedBug.setStatus(BugStatus.OPEN);
        UUID id = savedBug.getId();
        when(bugRepository.findById(id)).thenReturn(Optional.of(savedBug));
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);

        BugResponse response = bugService.updateStatus(id, new BugStatusUpdateRequest(BugStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(BugStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_shouldTransitionFromInProgressToResolved() {
        savedBug.setStatus(BugStatus.IN_PROGRESS);
        UUID id = savedBug.getId();
        when(bugRepository.findById(id)).thenReturn(Optional.of(savedBug));
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);

        BugResponse response = bugService.updateStatus(id, new BugStatusUpdateRequest(BugStatus.RESOLVED));

        assertThat(response.status()).isEqualTo(BugStatus.RESOLVED);
    }

    @Test
    void findById_shouldThrowBugNotFound_whenDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(bugRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bugService.findById(id))
                .isInstanceOf(BugNotFoundException.class);
    }

    @Test
    void findBySeverity_shouldReturnMatchingBugs() {
        when(bugRepository.findBySeverity(BugSeverity.CRITICAL)).thenReturn(List.of(savedBug));

        List<BugResponse> result = bugService.findBySeverity(BugSeverity.CRITICAL);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_shouldReturnAllBugs() {
        when(bugRepository.findAll()).thenReturn(List.of(savedBug));

        List<BugResponse> result = bugService.findAll();

        assertThat(result).hasSize(1);
    }
}
