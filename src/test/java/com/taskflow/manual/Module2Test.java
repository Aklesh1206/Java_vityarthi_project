package com.taskflow.manual;

import com.taskflow.model.Task;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.executor.TaskExecutorService;
import com.taskflow.logger.ConsoleTaskLogger;
import com.taskflow.exception.InvalidTaskException;

/**
 * Standalone smoke test for Module 2 (thread pool executor).
 * Submits a batch of tasks with mixed priorities, starts a small
 * thread pool, and lets it drain the queue concurrently.
 */
public class Module2Test {
    public static void main(String[] args) throws InvalidTaskException, InterruptedException {
        TaskQueueManager queueManager = new TaskQueueManager();
        ConsoleTaskLogger logger = new ConsoleTaskLogger();

        // submit 12 tasks with mixed priorities and durations
        for (int i = 1; i <= 12; i++) {
            int priority = (i % 3) + 1; // priorities 1,2,3 cycling
            long duration = 300 + (i % 4) * 200; // varied durations
            queueManager.submit(new Task("Job-" + i, priority, duration));
        }

        System.out.println("Submitted 12 tasks. Starting pool of 4 workers...\n");

        TaskExecutorService executorService = new TaskExecutorService(4);
        executorService.start(queueManager, logger);

        // let it run until the queue drains (simple polling wait for this test)
        while (!queueManager.isEmpty()) {
            Thread.sleep(200);
        }
        // give in-flight tasks a moment to finish
        Thread.sleep(1000);

        executorService.shutdown(5);

        System.out.println("\n--- Summary ---");
        System.out.println("Completed: " + executorService.getCompletedCount());
        System.out.println("Failed:    " + executorService.getFailedCount());
        System.out.println("Retried:   " + executorService.getRetriedCount());
    }
}
