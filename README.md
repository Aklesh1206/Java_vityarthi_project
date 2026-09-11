# TaskFlow — Multi-threaded Job Queue Simulator

## Overview
TaskFlow is a Java-based task scheduling simulator built for the "Programming in Java" course project. It simulates how a real-world job scheduler works: tasks are submitted with a priority, queued, and processed concurrently by a pool of worker threads. Every stage of a task's lifecycle is logged to a file, and a performance report is generated summarizing throughput, average wait/execution time, and failure rate.

## Features
- **Priority-based task queue** — tasks are processed by priority (1 = highest), with FIFO ordering for tasks of equal priority
- **Multi-threaded execution** — a configurable thread pool (`ExecutorService`) processes tasks concurrently, with thread-safe counters for completed/failed/retried tasks
- **Automatic retry on failure** — a task that fails is retried once before being marked permanently failed
- **File-based logging** — every task state transition (submitted, started, completed, failed, retrying) is logged with a timestamp and worker thread name to `taskflow-log.csv`
- **Performance report generation** — after a run, `taskflow-report.txt` is generated with total tasks, completion/failure rates, average wait time, average execution time, throughput (tasks/sec), and a per-task breakdown
- **Input validation & custom exception handling** — invalid task submissions (blank name, non-positive duration/priority) are rejected with `InvalidTaskException`; execution failures are handled with `TaskExecutionException`

## Technologies / Tools Used
- Java 21 (core language, no external dependencies)
- `java.util.concurrent` — `ExecutorService`, `PriorityBlockingQueue`, `AtomicInteger`, `CopyOnWriteArrayList`
- Plain file I/O (`FileWriter`, `PrintWriter`, `BufferedReader`) for logging and task input
- Graphviz (for generating design diagrams — not a runtime dependency)

## Project Structure
```
src/
 ├── model/            Task, TaskStatus (enum), TaskPriorityComparator
 ├── queue/            TaskQueueManager
 ├── executor/         TaskExecutorService, Worker
 ├── exception/        InvalidTaskException, TaskExecutionException
 ├── logger/           TaskLogger (interface), ConsoleTaskLogger, FileTaskLogger
 ├── report/           PerformanceReportGenerator
 ├── util/             ConfigLoader
 └── Main.java         Entry point
diagrams/              Architecture, workflow, class, and sequence diagrams
output/                Sample log and report from a test run
tasks.txt              Sample input file (task definitions)
```

## Steps to Install & Run
1. Ensure JDK 17+ is installed (`java -version`).
2. From the `src/` directory, compile all source files:
   ```
   javac model/*.java exception/*.java queue/*.java logger/*.java executor/*.java report/*.java util/*.java Main.java
   ```
3. Run the app, pointing it at a task file (defaults to `tasks.txt` in the current directory, pool size 4):
   ```
   java Main tasks.txt 4
   ```
4. Output files `taskflow-log.csv` and `taskflow-report.txt` are generated in the same directory.

### Task file format
Each line: `name,priority,estimatedDurationMillis` (priority: 1 = highest). Lines starting with `#` are comments.
```
Generate report,3,1200
Send email,1,400
```

## Instructions for Testing
An automated validation suite (`ValidationTests.java`) runs 20 assertions across the core modules — priority ordering, FIFO tiebreaking, submission validation, task timing, and a full concurrent execution run — with no external test framework required:
```
javac model/*.java exception/*.java queue/*.java logger/*.java executor/*.java report/*.java util/*.java ValidationTests.java
java ValidationTests
```
Exits with code 0 if all checks pass, 1 otherwise.

Three standalone test classes were also used during development to validate each module independently before wiring them together:
- `Module1Test.java` — verifies the priority queue orders tasks correctly and rejects invalid submissions
- `Module2Test.java` — verifies concurrent execution across multiple worker threads, with retry-on-failure behavior
- `Module3Test.java` — verifies the full pipeline end-to-end, including file logging and report generation

Compile and run any of these the same way as `Main.java` (e.g. `java Module1Test`) to see each module's output in isolation.

## Screenshots
See `output/sample-log.csv` and `output/sample-report.txt` for example output from a real run, and `diagrams/` for the architecture, workflow, class, and sequence diagrams.
