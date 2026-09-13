package com.taskflow.executor;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.logger.TaskLogger;
import com.taskflow.exception.TaskExecutionException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A single worker thread's job: repeatedly take the highest-priority
 * task from the queue, "execute" it, and handle failure/retry.
 *
 * Each worker runs this as its own Runnable inside the thread pool,
 * so with N threads you get N of these running concurrently, all
 * pulling from the same thread-safe TaskQueueManager.
 */
public class Worker implements Runnable {

    private static final int MAX_RETRIES = 1;
    // simulated failure chance, purely to demonstrate retry/exception handling
    private static final double SIMULATED_FAILURE_RATE = 0.15;

    private final TaskQueueManager queueManager;
    private final TaskLogger logger;
    private final AtomicInteger completedCount;
    private final AtomicInteger failedCount;
    private final AtomicInteger retriedCount;
    private final List<Task> processedTasks; // thread-safe list (e.g. CopyOnWriteArrayList), feeds the report generator

    public Worker(TaskQueueManager queueManager,
                  TaskLogger logger,
                  AtomicInteger completedCount,
                  AtomicInteger failedCount,
                  AtomicInteger retriedCount,
                  List<Task> processedTasks) {
        this.queueManager = queueManager;
        this.logger = logger;
        this.completedCount = completedCount;
        this.failedCount = failedCount;
        this.retriedCount = retriedCount;
        this.processedTasks = processedTasks;
    }

    @Override
    public void run() {
        // Loop until the thread is interrupted (that's how we signal shutdown,
        // since queueManager.takeNext() blocks when the queue is empty).
        while (!Thread.currentThread().isInterrupted()) {
            Task task;
            try {
                task = queueManager.takeNext();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore interrupt status
                break; // exit loop cleanly on shutdown
            }

            processWithRetry(task);
        }
    }

    private void processWithRetry(Task task) {
        task.markStarted();
        logger.logEvent(task, "STARTED on " + Thread.currentThread().getName());

        try {
            execute(task);
            task.markCompleted();
            completedCount.incrementAndGet();
            logger.logEvent(task, "COMPLETED");
            processedTasks.add(task);
        } catch (TaskExecutionException firstFailure) {
            logger.logEvent(task, "FAILED (attempt 1): " + firstFailure.getMessage());

            if (task.getRetryCount() < MAX_RETRIES) {
                task.incrementRetryCount();
                task.setStatus(TaskStatus.RETRYING);
                retriedCount.incrementAndGet();
                logger.logEvent(task, "RETRYING");

                try {
                    execute(task);
                    task.markCompleted();
                    completedCount.incrementAndGet();
                    logger.logEvent(task, "COMPLETED on retry");
                    processedTasks.add(task);
                } catch (TaskExecutionException secondFailure) {
                    task.markFailed();
                    failedCount.incrementAndGet();
                    logger.logEvent(task, "FAILED permanently: " + secondFailure.getMessage());
                    processedTasks.add(task);
                }
            } else {
                task.markFailed();
                failedCount.incrementAndGet();
                logger.logEvent(task, "FAILED permanently: " + firstFailure.getMessage());
                processedTasks.add(task);
            }
        }
    }

    /**
     * Simulates doing the task's actual work. In a real system this is
     * where you'd call the task's business logic; here we sleep for the
     * estimated duration and randomly throw to simulate real-world failures
     * (timeouts, bad input, external service errors, etc.).
     */
    private void execute(Task task) throws TaskExecutionException {
        try {
            Thread.sleep(task.getEstimatedDurationMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException("Interrupted while executing task " + task.getId(), e);
        }

        if (ThreadLocalRandom.current().nextDouble() < SIMULATED_FAILURE_RATE) {
            throw new TaskExecutionException("Simulated random failure for task " + task.getId());
        }
    }
}
