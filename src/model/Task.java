package model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single unit of work submitted to the scheduler.
 *
 * priority: lower number = higher priority (1 = highest), matching
 * standard OS scheduling convention. Adjust in TaskPriorityComparator
 * if you'd rather have higher number = higher priority.
 */
public class Task {

    // simple auto-incrementing id generator, thread-safe
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final long id;
    private final String name;
    private final int priority;
    private final long estimatedDurationMillis;

    private volatile TaskStatus status;
    private final Instant submittedAt;
    private Instant startedAt;
    private Instant completedAt;

    private int retryCount;

    public Task(String name, int priority, long estimatedDurationMillis) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
        this.priority = priority;
        this.estimatedDurationMillis = estimatedDurationMillis;
        this.status = TaskStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.retryCount = 0;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public long getEstimatedDurationMillis() {
        return estimatedDurationMillis;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void markStarted() {
        this.startedAt = Instant.now();
        this.status = TaskStatus.RUNNING;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
        this.status = TaskStatus.COMPLETED;
    }

    public void markFailed() {
        this.completedAt = Instant.now();
        this.status = TaskStatus.FAILED;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    /** Wait time = how long the task sat in queue before a worker picked it up. */
    public long getWaitTimeMillis() {
        if (startedAt == null) return -1;
        return startedAt.toEpochMilli() - submittedAt.toEpochMilli();
    }

    /** Execution time = how long the worker actually spent running it. */
    public long getExecutionTimeMillis() {
        if (startedAt == null || completedAt == null) return -1;
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format("Task[id=%d, name=%s, priority=%d, status=%s]",
                id, name, priority, status);
    }
}
