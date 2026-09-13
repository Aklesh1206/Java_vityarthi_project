package com.taskflow;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.queue.TaskQueueManager;
import com.taskflow.executor.TaskExecutorService;
import com.taskflow.logger.ConsoleTaskLogger;
import com.taskflow.exception.InvalidTaskException;

import java.util.List;

/**
 * Automated validation test suite for TaskFlow's core modules.
 *
 * This is a lightweight, dependency-free test runner (no JUnit required)
 * that exercises the priority queue, validation logic, task timing
 * calculations, and a full concurrent execution run — then reports
 * PASS/FAIL for each check plus a summary.
 *
 * Run with: java com.taskflow.TaskFlowValidationTest
 * Exit code is 0 if all tests pass, 1 otherwise (useful for CI).
 */
public class TaskFlowValidationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("Running TaskFlow validation tests...\n");

        testPriorityOrderingAndFifoTiebreak();
        testInvalidTaskRejection();
        testQueueSizeAndEmptyState();
        testTaskTimingBeforeAndAfterExecution();
        testFullConcurrentRunAccountsForAllTasks();

        System.out.println("\n--- Summary ---");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.out.println("[FAIL] " + testName);
            failed++;
        }
    }

    /** Verifies tasks come out in priority order, with FIFO tiebreak for equal priorities. */
    private static void testPriorityOrderingAndFifoTiebreak() throws InvalidTaskException, InterruptedException {
        TaskQueueManager qm = new TaskQueueManager();
        qm.submit(new Task("Low-priority job", 3, 100));
        qm.submit(new Task("First high-priority job", 1, 100));
        qm.submit(new Task("Mid-priority job", 2, 100));
        qm.submit(new Task("Second high-priority job", 1, 100)); // same priority as above, submitted later

        Task first = qm.takeNext();
        Task second = qm.takeNext();
        Task third = qm.takeNext();
        Task fourth = qm.takeNext();

        check("Priority ordering: highest priority (1) comes out first",
                first.getPriority() == 1 && first.getName().equals("First high-priority job"));
        check("Priority ordering: FIFO tiebreak between equal priorities",
                second.getPriority() == 1 && second.getName().equals("Second high-priority job"));
        check("Priority ordering: mid priority (2) comes third",
                third.getPriority() == 2);
        check("Priority ordering: low priority (3) comes last",
                fourth.getPriority() == 3);
    }

    /** Verifies invalid submissions are rejected with the correct exception. */
    private static void testInvalidTaskRejection() {
        TaskQueueManager qm = new TaskQueueManager();
        boolean blankNameRejected = false;
        boolean zeroPriorityRejected = false;
        boolean negativeDurationRejected = false;

        try {
            qm.submit(new Task("", 1, 100));
        } catch (InvalidTaskException e) {
            blankNameRejected = true;
        }

        try {
            qm.submit(new Task("Valid name", 0, 100));
        } catch (InvalidTaskException e) {
            zeroPriorityRejected = true;
        }

        try {
            qm.submit(new Task("Valid name", 1, -50));
        } catch (InvalidTaskException e) {
            negativeDurationRejected = true;
        }

        check("Validation: blank task name is rejected", blankNameRejected);
        check("Validation: priority < 1 is rejected", zeroPriorityRejected);
        check("Validation: non-positive duration is rejected", negativeDurationRejected);
    }

    /** Verifies queue size/isEmpty report correctly as tasks are added and removed. */
    private static void testQueueSizeAndEmptyState() throws InvalidTaskException, InterruptedException {
        TaskQueueManager qm = new TaskQueueManager();
        boolean emptyInitially = qm.isEmpty();

        qm.submit(new Task("Job A", 1, 50));
        qm.submit(new Task("Job B", 2, 50));
        boolean sizeIsTwo = qm.size() == 2;

        qm.takeNext();
        boolean sizeIsOneAfterTake = qm.size() == 1;

        qm.takeNext();
        boolean emptyAfterDraining = qm.isEmpty();

        check("Queue: reports empty before any submission", emptyInitially);
        check("Queue: size reflects number of submitted tasks", sizeIsTwo);
        check("Queue: size decreases after takeNext()", sizeIsOneAfterTake);
        check("Queue: reports empty after draining all tasks", emptyAfterDraining);
    }

    /** Verifies wait/execution time calculations behave correctly at each lifecycle stage. */
    private static void testTaskTimingBeforeAndAfterExecution() throws InterruptedException {
        Task task = new Task("Timing test job", 1, 100);
        boolean waitTimeIsNegativeBeforeStart = task.getWaitTimeMillis() == -1;
        boolean execTimeIsNegativeBeforeCompletion = task.getExecutionTimeMillis() == -1;

        Thread.sleep(20); // simulate time spent waiting in queue
        task.markStarted();
        boolean statusIsRunning = task.getStatus() == TaskStatus.RUNNING;
        boolean waitTimeIsNonNegativeAfterStart = task.getWaitTimeMillis() >= 0;

        Thread.sleep(20); // simulate execution time
        task.markCompleted();
        boolean statusIsCompleted = task.getStatus() == TaskStatus.COMPLETED;
        boolean execTimeIsPositiveAfterCompletion = task.getExecutionTimeMillis() > 0;

        check("Timing: wait time is -1 before task starts", waitTimeIsNegativeBeforeStart);
        check("Timing: execution time is -1 before task completes", execTimeIsNegativeBeforeCompletion);
        check("Timing: status becomes RUNNING after markStarted()", statusIsRunning);
        check("Timing: wait time is non-negative once started", waitTimeIsNonNegativeAfterStart);
        check("Timing: status becomes COMPLETED after markCompleted()", statusIsCompleted);
        check("Timing: execution time is positive after completion", execTimeIsPositiveAfterCompletion);
    }

    /** Integration check: runs a small batch through the real thread pool and verifies accounting is consistent. */
    private static void testFullConcurrentRunAccountsForAllTasks() throws Exception {
        TaskQueueManager qm = new TaskQueueManager();
        int totalTasks = 10;
        for (int i = 1; i <= totalTasks; i++) {
            qm.submit(new Task("Integration job " + i, (i % 3) + 1, 50));
        }

        TaskExecutorService executorService = new TaskExecutorService(3);
        executorService.start(qm, new ConsoleTaskLogger());

        while (!qm.isEmpty()) {
            Thread.sleep(50);
        }
        Thread.sleep(500); // let in-flight tasks finish
        executorService.shutdown(5);

        int completed = executorService.getCompletedCount();
        int failedCount = executorService.getFailedCount();
        List<Task> processed = executorService.getProcessedTasks();

        check("Concurrency: every submitted task reaches a final state",
                processed.size() == totalTasks);
        check("Concurrency: completed + failed counts add up to total tasks",
                (completed + failedCount) == totalTasks);
        check("Concurrency: no task is left in a non-final status",
                processed.stream().allMatch(t ->
                        t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.FAILED));
    }
}
