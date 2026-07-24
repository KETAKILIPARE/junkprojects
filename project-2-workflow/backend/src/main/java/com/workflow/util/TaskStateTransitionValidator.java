package com.workflow.util;

import com.workflow.domain.TaskStatus;

public final class TaskStateTransitionValidator {

    private TaskStateTransitionValidator() {}

    public static boolean isValid(TaskStatus from, TaskStatus to) {
        return from != to;
    }
}
