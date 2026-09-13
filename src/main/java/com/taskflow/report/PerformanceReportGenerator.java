package com.taskflow.report;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

/**
 * Computes and writes a performance summary report from a batch of
 * processed tasks: throughput, average wait time, average execution
 * time, and failure rate.
 */
public class PerformanceReportGenerator {

    /**
     * Generates the report and writes it to the given file path.
     *
     * @param tasks       tasks that reached a final state (completed/failed)
     * @param runDurationMillis wall-clock time the whole batch took to process,
     *                          used to compute throughput (tasks/sec)
     */
    public void generate(List<Task> tasks, long runDurationMillis, String filePath) throws IOException {
        int total = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long failed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.FAILED).count();

        double avgWaitMillis = tasks.stream()
                .mapToLong(Task::getWaitTimeMillis)
                .filter(w -> w >= 0)
                .average()
                .orElse(0.0);

        double avgExecMillis = tasks.stream()
                .mapToLong(Task::getExecutionTimeMillis)
                .filter(e -> e >= 0)
                .average()
                .orElse(0.0);

        double throughputPerSec = runDurationMillis > 0
                ? total / (runDurationMillis / 1000.0)
                : 0.0;

        double failureRatePct = total > 0 ? (failed * 100.0 / total) : 0.0;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false), true)) {
            writer.println("=== TaskFlow Performance Report ===");
            writer.println("Generated at: " + Instant.now());
            writer.println();
            writer.println("Total tasks processed : " + total);
            writer.println("Completed              : " + completed);
            writer.println("Failed                 : " + failed);
            writer.printf("Failure rate           : %.2f%%%n", failureRatePct);
            writer.println();
            writer.printf("Average wait time      : %.2f ms%n", avgWaitMillis);
            writer.printf("Average execution time : %.2f ms%n", avgExecMillis);
            writer.printf("Throughput             : %.2f tasks/sec%n", throughputPerSec);
            writer.println();
            writer.println("Run duration           : " + runDurationMillis + " ms");
            writer.println();
            writer.println("=== Per-task Breakdown ===");
            writer.println("TaskID,Name,Priority,Status,WaitMs,ExecMs,Retries");
            for (Task t : tasks) {
                writer.printf("%d,%s,%d,%s,%d,%d,%d%n",
                        t.getId(), t.getName(), t.getPriority(), t.getStatus(),
                        t.getWaitTimeMillis(), t.getExecutionTimeMillis(), t.getRetryCount());
            }
        }
    }
}
