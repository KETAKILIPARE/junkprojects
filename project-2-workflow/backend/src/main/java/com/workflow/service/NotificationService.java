package com.workflow.service;

import com.workflow.dto.TaskNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastTaskUpdate(TaskNotification notification) {
        messagingTemplate.convertAndSend(
                "/topic/workspace/" + notification.workspaceId(),
                notification
        );
    }
}
