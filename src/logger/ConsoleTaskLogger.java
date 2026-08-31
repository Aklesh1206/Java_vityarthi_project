package logger;

import model.Task;
import java.time.Instant;

/**
 * Simple console logger used for testing/dev. Module 3 will add
 * FileTaskLogger (implements the same TaskLogger interface) for the
 * real file-based logging + performance report requirement.
 */
public class ConsoleTaskLogger implements TaskLogger {
    @Override
    public void logEvent(Task task, String event) {
        System.out.printf("[%s] Task#%d (%s) -> %s%n",
                Instant.now(), task.getId(), task.getName(), event);
    }
}
