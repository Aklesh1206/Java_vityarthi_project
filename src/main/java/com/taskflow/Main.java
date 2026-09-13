package com.taskflow;

import com.taskflow.model.Task;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.executor.TaskExecutorService;
import com.taskflow.logger.FileTaskLogger;
import com.taskflow.report.PerformanceReportGenerator;
import com.taskflow.exception.InvalidTaskException;
import com.taskflow.util.ConfigLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Entry point for TaskFlow.
 *
 * Reads task definitions from tasks.txt (format: name,priority,durationMillis
 * per line), submits them to the queue, processes them through a
 * configurable thread pool, and writes a log file + performance report.
 *
 * Usage: java com.taskflow.Main [tasksFile] [poolSize]
 * Defaults: tasksFile = "config/tasks.txt", poolSize = 4
 */
public class Main {

    public static void main(String[] args) {
        String tasksFile = args.length > 0 ? args[0] : "config/tasks.txt";
        int poolSize = args.length > 1 ? Integer.parseInt(args[1]) : ConfigLoader.getDefaultPoolSize();

        TaskQueueManager queueManager = new TaskQueueManager();

        int submitted = 0;
        int rejected = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(tasksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip blanks/comments

                String[] parts = line.split(",");
                if (parts.length != 3) {
                    System.err.println("Skipping malformed line: " + line);
                    rejected++;
                    continue;
                }

                try {
                    String name = parts[0].trim();
                    int priority = Integer.parseInt(parts[1].trim());
                    long duration = Long.parseLong(parts[2].trim());

                    queueManager.submit(new Task(name, priority, duration));
                    submitted++;
                } catch (NumberFormatException | InvalidTaskException e) {
                    System.err.println("Rejected task from line \"" + line + "\": " + e.getMessage());
                    rejected++;
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read tasks file '" + tasksFile + "': " + e.getMessage());
            System.err.println("Create a tasks.txt file with lines like: Generate report,2,1500");
            return;
        }

        System.out.println("Loaded " + submitted + " tasks (" + rejected + " rejected). Pool size: " + poolSize);

        if (submitted == 0) {
            System.out.println("No valid tasks to process. Exiting.");
            return;
        }

        try (FileTaskLogger fileLogger = new FileTaskLogger("taskflow-log.csv")) {
            TaskExecutorService executorService = new TaskExecutorService(poolSize);

            long startTime = System.currentTimeMillis();
            executorService.start(queueManager, fileLogger);

            while (!queueManager.isEmpty()) {
                Thread.sleep(200);
            }
            Thread.sleep(1000); // allow in-flight tasks to finish

            executorService.shutdown(10);
            long endTime = System.currentTimeMillis();

            System.out.println("Processing complete:");
            System.out.println("  Completed: " + executorService.getCompletedCount());
            System.out.println("  Failed:    " + executorService.getFailedCount());
            System.out.println("  Retried:   " + executorService.getRetriedCount());

            PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();
            reportGenerator.generate(executorService.getProcessedTasks(), endTime - startTime, "taskflow-report.txt");

            System.out.println("Log written to taskflow-log.csv");
            System.out.println("Report written to taskflow-report.txt");

        } catch (Exception e) {
            System.err.println("Fatal error during processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
