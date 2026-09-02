import model.Task;
import queue.TaskQueueManager;
import executor.TaskExecutorService;
import logger.FileTaskLogger;
import report.PerformanceReportGenerator;
import exception.InvalidTaskException;

/**
 * Full end-to-end run of TaskFlow: submits tasks, processes them through
 * the thread pool with file-based logging, then generates a performance
 * report. This is effectively what Main.java will do in the final app.
 */
public class Module3Test {
    public static void main(String[] args) throws Exception {
        TaskQueueManager queueManager = new TaskQueueManager();

        // submit a batch of tasks with varied priority/duration
        for (int i = 1; i <= 15; i++) {
            int priority = (i % 3) + 1;
            long duration = 200 + (i % 5) * 150;
            queueManager.submit(new Task("Job-" + i, priority, duration));
        }
        System.out.println("Submitted 15 tasks.");

        try (FileTaskLogger fileLogger = new FileTaskLogger("taskflow-log.csv")) {
            TaskExecutorService executorService = new TaskExecutorService(4);

            long startTime = System.currentTimeMillis();
            executorService.start(queueManager, fileLogger);

            while (!queueManager.isEmpty()) {
                Thread.sleep(200);
            }
            Thread.sleep(1000); // let in-flight tasks finish

            executorService.shutdown(5);
            long endTime = System.currentTimeMillis();

            System.out.println("Processing complete. Generating report...");

            PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();
            reportGenerator.generate(
                    executorService.getProcessedTasks(),
                    endTime - startTime,
                    "taskflow-report.txt"
            );

            System.out.println("Done. See taskflow-log.csv and taskflow-report.txt");
            System.out.println("Completed: " + executorService.getCompletedCount()
                    + " | Failed: " + executorService.getFailedCount()
                    + " | Retried: " + executorService.getRetriedCount());
        }
    }
}
