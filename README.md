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
- Java 17+ (core language, no external runtime dependencies)
- Maven (standard project layout and build lifecycle)
- `java.util.concurrent` — `ExecutorService`, `PriorityBlockingQueue`, `AtomicInteger`, `CopyOnWriteArrayList`
- Plain file I/O (`FileWriter`, `PrintWriter`, `BufferedReader`) for logging and task input
- GitHub Actions (CI — compiles and runs the validation suite on every push)
- Graphviz + Python/Pillow (used to generate the design diagrams — not a runtime dependency)

## Project Structure
```
TaskFlow/
├── pom.xml                      Maven project descriptor
├── .github/workflows/build.yml  CI: compiles + runs validation suite on every push
├── config/
│   └── tasks.txt                 Sample task input file
├── src/
│   ├── main/java/com/taskflow/
│   │   ├── Main.java              Entry point
│   │   ├── model/                 Task, TaskStatus (enum), TaskPriorityComparator
│   │   ├── queue/                 TaskQueueManager
│   │   ├── executor/               TaskExecutorService, Worker
│   │   ├── exception/              InvalidTaskException, TaskExecutionException
│   │   ├── logger/                 TaskLogger (interface), ConsoleTaskLogger, FileTaskLogger
│   │   ├── report/                 PerformanceReportGenerator
│   │   └── util/                   ConfigLoader
│   └── test/java/com/taskflow/
│       ├── TaskFlowValidationTest.java   Automated validation suite (20 assertions)
│       └── manual/                       Module1Test, Module2Test, Module3Test — dev-time smoke tests
├── docs/
│   ├── TaskFlow_Project_Report.pdf      Full 15-section project report
│   ├── diagrams/                         Architecture, workflow, class, sequence, use case diagrams
│   └── sample-output/                    Sample log + report from a real run
├── README.md
└── statement.md
```

## Steps to Install & Run

### Option A — Maven (recommended)
1. Ensure JDK 17+ and Maven are installed (`java -version`, `mvn -version`).
2. From the project root, compile:
   ```
   mvn compile
   ```
3. Run the app (uses `config/tasks.txt` and pool size 4 by default, configured in `pom.xml`):
   ```
   mvn exec:java
   ```
   To use different arguments: `mvn exec:java -Dexec.args="config/tasks.txt 6"`
4. Or build a runnable jar and run it directly:
   ```
   mvn package
   java -jar target/taskflow.jar config/tasks.txt 4
   ```

### Option B — Plain javac (no Maven required)
From the project root:
```
find src/main/java -name "*.java" > sources.txt
javac -d target/classes @sources.txt
java -cp target/classes com.taskflow.Main config/tasks.txt 4
```

Output files `taskflow-log.csv` and `taskflow-report.txt` are generated in the current working directory.

### Task file format
Each line: `name,priority,estimatedDurationMillis` (priority: 1 = highest). Lines starting with `#` are comments.
```
Generate report,3,1200
Send email,1,400
```

## Instructions for Testing
An automated validation suite (`TaskFlowValidationTest.java`) runs 20 assertions across the core modules — priority ordering, FIFO tiebreaking, submission validation, task timing, and a full concurrent execution run. It's a plain, dependency-free runner (no JUnit) so it always compiles offline; see the note in `pom.xml` if you'd like to wire it up as a proper JUnit suite for `mvn test`.

Run it with plain javac/java:
```
find src/test/java -name "*.java" > test-sources.txt
javac -cp target/classes -d target/test-classes @test-sources.txt
java -cp target/classes:target/test-classes com.taskflow.TaskFlowValidationTest
```
Exits with code 0 if all checks pass, 1 otherwise — this exact sequence runs automatically in CI on every push (see `.github/workflows/build.yml`).

Three standalone test classes under `src/test/java/com/taskflow/manual/` were also used during development to validate each module independently before wiring them together:
- `Module1Test` — verifies the priority queue orders tasks correctly and rejects invalid submissions
- `Module2Test` — verifies concurrent execution across multiple worker threads, with retry-on-failure behavior
- `Module3Test` — verifies the full pipeline end-to-end, including file logging and report generation

## Screenshots
See `docs/sample-output/` for example output from a real run, and `docs/diagrams/` for the architecture, workflow, class, sequence, and use case diagrams. The full write-up is in `docs/TaskFlow_Project_Report.pdf`.
