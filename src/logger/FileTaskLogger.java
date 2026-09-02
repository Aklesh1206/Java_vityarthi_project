package logger;

import model.Task;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * Writes each task lifecycle event to a log file. Multiple worker threads
 * call logEvent() concurrently, so writes are synchronized to avoid
 * interleaved/corrupted lines in the file.
 */
public class FileTaskLogger implements TaskLogger, AutoCloseable {

    private final PrintWriter writer;

    public FileTaskLogger(String filePath) throws IOException {
        // append=false: start fresh each run
        this.writer = new PrintWriter(new FileWriter(filePath, false), true);
        writer.println("timestamp,taskId,taskName,thread,event");
    }

    @Override
    public synchronized void logEvent(Task task, String event) {
        // CSV-style line: easy to parse later if needed, still human-readable
        writer.printf("%s,%d,%s,%s,%s%n",
                Instant.now(),
                task.getId(),
                task.getName(),
                Thread.currentThread().getName(),
                event);
    }

    @Override
    public synchronized void close() {
        writer.flush();
        writer.close();
    }
}
