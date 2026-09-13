package com.taskflow.logger;

import com.taskflow.model.Task;

/**
 * Minimal logging contract. Module 3 (Logging & Performance Report Generator)
 * will provide a full file-based implementation (FileTaskLogger). For now,
 * ConsoleTaskLogger lets us test the executor independently.
 */
public interface TaskLogger {
    void logEvent(Task task, String event);
}
