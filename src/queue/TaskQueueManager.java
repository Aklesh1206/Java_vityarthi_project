package queue;

import model.Task;
import model.TaskPriorityComparator;
import model.TaskStatus;
import exception.InvalidTaskException;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Wraps a PriorityBlockingQueue<Task> and handles validation on submission.
 * PriorityBlockingQueue is already thread-safe, so multiple producer threads
 * can call submit() concurrently and multiple worker threads can call take()
 * concurrently without extra synchronization here.
 */
public class TaskQueueManager {

    private final PriorityBlockingQueue<Task> queue;

    public TaskQueueManager() {
        // initial capacity 11 (default), comparator does the ordering work
        this.queue = new PriorityBlockingQueue<>(11, new TaskPriorityComparator());
    }

    /**
     * Validates and submits a task to the queue.
     * Throws InvalidTaskException if the task fails basic validation rules.
     */
    public void submit(Task task) throws InvalidTaskException {
        validate(task);
        task.setStatus(TaskStatus.QUEUED);
        queue.put(task);
    }

    private void validate(Task task) throws InvalidTaskException {
        if (task == null) {
            throw new InvalidTaskException("Task cannot be null.");
        }
        if (task.getName() == null || task.getName().isBlank()) {
            throw new InvalidTaskException("Task name cannot be blank (task id=" + task.getId() + ").");
        }
        if (task.getPriority() < 1) {
            throw new InvalidTaskException("Task priority must be >= 1 (task id=" + task.getId() + ").");
        }
        if (task.getEstimatedDurationMillis() <= 0) {
            throw new InvalidTaskException("Task duration must be positive (task id=" + task.getId() + ").");
        }
    }

    /**
     * Blocks until a task is available, then removes and returns the
     * highest-priority one. Called by worker threads.
     */
    public Task takeNext() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
