package exception;

/**
 * Thrown when a task submission fails validation
 * (e.g. blank name, negative priority, non-positive duration).
 */
public class InvalidTaskException extends Exception {

    public InvalidTaskException(String message) {
        super(message);
    }

    public InvalidTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
