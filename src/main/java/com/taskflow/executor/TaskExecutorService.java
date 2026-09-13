package com.taskflow.executor;

import com.taskflow.model.Task;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.logger.TaskLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the thread pool and coordinates N Worker instances, each pulling
 * from the same TaskQueueManager concurrently. This is the concurrency
 * core of the whole project.
 */
public class TaskExecutorService {

    private final ExecutorService pool;
    private final int poolSize;

    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger retriedCount = new AtomicInteger(0);
    // CopyOnWriteArrayList: safe for many concurrent writer threads (workers)
    // with occasional reads (report generation happens once, at the end)
    private final List<Task> processedTasks = new CopyOnWriteArrayList<>();

    public TaskExecutorService(int poolSize) {
        this.poolSize = poolSize;
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Starts poolSize worker threads, each independently looping and
     * pulling tasks from queueManager until shutdown() is called.
     */
    public void start(TaskQueueManager queueManager, TaskLogger logger) {
        for (int i = 0; i < poolSize; i++) {
            pool.submit(new Worker(queueManager, logger, completedCount, failedCount, retriedCount, processedTasks));
        }
    }

    /** Returns the tasks that reached a final state (completed or failed), for reporting. */
    public List<Task> getProcessedTasks() {
        return processedTasks;
    }

    /**
     * Gracefully shuts down: stops accepting new work, interrupts blocked
     * workers (they're blocked on queue.take()), and waits up to the given
     * timeout for in-flight tasks to finish.
     */
    public void shutdown(long timeoutSeconds) {
        pool.shutdownNow(); // interrupts all worker threads, breaking their take() calls
        try {
            if (!pool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getCompletedCount() {
        return completedCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }

    public int getRetriedCount() {
        return retriedCount.get();
    }
}
