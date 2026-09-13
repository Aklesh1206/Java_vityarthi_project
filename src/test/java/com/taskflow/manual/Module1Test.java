package com.taskflow.manual;

import com.taskflow.model.Task;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.exception.InvalidTaskException;

/**
 * Standalone smoke test for Module 1 (submission + priority queue).
 * Not part of the final app — just proves the queue orders correctly
 * before we wire in the thread pool executor in Module 2.
 */
public class Module1Test {
    public static void main(String[] args) throws InterruptedException {
        TaskQueueManager queueManager = new TaskQueueManager();

        try {
            queueManager.submit(new Task("Generate report", 3, 2000));
            queueManager.submit(new Task("Send email", 1, 500));
            queueManager.submit(new Task("Backup database", 2, 3000));
            queueManager.submit(new Task("Cleanup temp files", 1, 1000)); // same priority as "Send email" -> FIFO tiebreak

            // invalid task -> should throw and be caught
            queueManager.submit(new Task("", 1, 100));
        } catch (InvalidTaskException e) {
            System.out.println("Caught expected validation error: " + e.getMessage());
        }

        System.out.println("Queue size: " + queueManager.size());
        System.out.println("Processing order (should be priority 1,1,2,3 with FIFO tiebreak):");
        while (!queueManager.isEmpty()) {
            Task next = queueManager.takeNext();
            System.out.println(" -> " + next);
        }
    }
}
