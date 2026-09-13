package com.taskflow.model;

import java.util.Comparator;

/**
 * Orders tasks by priority (lower number = higher priority).
 * Ties are broken by submission time (earlier submitted = goes first),
 * so the queue behaves as FIFO within the same priority level.
 */
public class TaskPriorityComparator implements Comparator<Task> {

    @Override
    public int compare(Task t1, Task t2) {
        int priorityCompare = Integer.compare(t1.getPriority(), t2.getPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // same priority -> earlier submission wins
        return t1.getSubmittedAt().compareTo(t2.getSubmittedAt());
    }
}
