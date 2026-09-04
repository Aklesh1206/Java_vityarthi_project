# Problem Statement

Modern systems — from CI/CD pipelines to background job processors — need to handle many units of work concurrently while respecting priority and reliably recovering from failures. Naively processing tasks one at a time, or without prioritization, wastes resources and delays high-priority work. There's a need for a lightweight scheduler that can queue tasks by priority, execute them concurrently across a bounded pool of workers, handle transient failures gracefully, and provide visibility into how the system performed.

# Scope of the Project

TaskFlow is a simulation of such a scheduler, built to demonstrate core and concurrent Java programming concepts for the "Programming in Java" course. It is not a distributed or persistent system — tasks are defined in a simple text file, processed in a single JVM instance using a thread pool, and results are written to local log and report files. The scope covers:
- Task submission with validation
- Priority-based queueing with FIFO tie-breaking
- Concurrent execution via a configurable thread pool
- Automatic single retry on task failure
- File-based event logging
- Post-run performance reporting

Out of scope: distributed execution across multiple machines, persistent/database-backed queues, and a graphical user interface.

# Target Users

- Students and developers learning Java concurrency (`ExecutorService`, thread-safe collections, synchronization)
- Anyone wanting a minimal, understandable reference implementation of a priority-based job scheduler before building something more complex (e.g. with a message broker or distributed queue)

# High-Level Features

1. **Task Submission & Priority Queueing** — tasks are validated and placed into a thread-safe `PriorityBlockingQueue`, ordered by priority and submission time.
2. **Multi-threaded Task Executor** — a fixed-size thread pool of workers concurrently pulls and processes tasks, with thread-safe tracking of completed/failed/retried counts and automatic retry-once-on-failure.
3. **Logging & Performance Report Generator** — every task lifecycle event is written to a CSV log file, and a final report summarizes throughput, average wait time, average execution time, and failure rate.
