package model;

/**
 * Represents the lifecycle state of a Task as it moves through the system.
 */
public enum TaskStatus {
    SUBMITTED,   // task created and added to queue
    QUEUED,      // waiting for a worker thread
    RUNNING,     // currently being executed by a worker
    COMPLETED,   // finished successfully
    FAILED,      // failed even after retry
    RETRYING     // failed once, being retried
}
