package exception;

/**
 * Thrown when a task fails during execution (as opposed to failing
 * validation at submission time, which is InvalidTaskException).
 */
public class TaskExecutionException extends Exception {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
