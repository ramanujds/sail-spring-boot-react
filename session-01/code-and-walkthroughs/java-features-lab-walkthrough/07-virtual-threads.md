# Step 7 — Virtual threads

> [Back to index](README.md) · Previous: [Step 6 — Text blocks](06-text-blocks.md) · Next: [Step 8 — Recap and exercises](08-recap-and-exercises.md)

**Package:** `virtualthreads` · **TODO:** 5.1 · **Test:** `VirtualThreadsLabTest`

## Goal

Run the same blocking tasks on virtual threads and measure the difference against a small pool of platform threads.

## Why this matters

A Spring MVC application handles each request on one thread, and that thread is held for the whole request. When the request makes a blocking call, such as a JDBC query to Oracle, the thread does nothing while it waits. Platform threads are expensive, so a server keeps a small pool, and the pool size becomes the cap on how many requests can be waiting at once. Raising the cap means more memory and more tuning.

A virtual thread is cheap enough to create one per task. When it blocks, the JVM parks it and lets the underlying platform thread run something else. The code stays ordinary blocking code, and the pool-size question goes away.

In Spring Boot 3.2 or later you get this for request handling with one property, which you set in the project on Day 1:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

The experiment in this step makes the effect visible with sleeps in place of database calls: 500 tasks that each block for 100 ms. Twenty platform threads need about `500 / 20 x 100 ms` of waiting. One virtual thread per task needs about the length of a single task.

> **Note:** Virtual threads help when threads spend their time waiting. They do not make CPU-bound work faster.

## 1. Run the lab

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.virtualthreads.VirtualThreadsLab
```

**Expected** (timings vary by machine):

```text
Tasks: 500, each blocks for 100 ms
Platform threads (pool of 20): 2611 ms
TODO 5.1: implement runWithVirtualThreads
platform: Thread[#45,platform-demo,5,main]
virtual:  VirtualThread[#47,virtual-demo]/runnable@ForkJoinPool-1-worker-1 isVirtual=true
```

Read the last two lines. A platform thread prints as `Thread[...]`. A virtual thread prints as `VirtualThread[...]` with the carrier thread it currently runs on, and `isVirtual()` returns `true`. This is the comparison you make again in the Spring Boot project, in the log, before and after you switch the property on.

## 2. TODO 5.1: `runWithVirtualThreads`

Open `VirtualThreadsLab.java`. `runWithPlatformThreads` is provided. Your method does the same work on virtual threads. The contract is:

| Requirement | Detail |
|---|---|
| Work | `tasks` tasks, each blocking for `work` |
| Threads | One virtual thread per task, with no pool size |
| Completion | Do not return until every task has finished |
| Result | The elapsed time in milliseconds |

Keep the structure close to the platform version: only how the executor is created should differ.

## 3. Play around

After the test is green:

1. Change `PLATFORM_POOL_SIZE` from 20 to 200, then 2000. At what size does the platform version catch up, and what does that cost?
2. Raise `tasks` in `main` to 10,000. Which version still finishes in about a second?
3. Inside a virtual task, print `Thread.currentThread()`. How many different carrier threads do you see?
4. Replace the sleep in a task with a loop that does arithmetic for about 100 ms. Compare both versions again. What happened to the gap?

## Try it

```bash
mvn test -Dtest=VirtualThreadsLabTest
```

**Expected:**

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The test runs 1,000 tasks of 100 ms and requires the result to be under two seconds. A 20-thread pool needs about five.

Then the lab:

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.virtualthreads.VirtualThreadsLab
```

**Expected** (timings vary by machine):

```text
Tasks: 500, each blocks for 100 ms
Platform threads (pool of 20): 2604 ms
Virtual threads: 112 ms
platform: Thread[#555,platform-demo,5,main]
virtual:  VirtualThread[#556,virtual-demo]/runnable@ForkJoinPool-1-worker-8 isVirtual=true
```

## Checkpoint

This is `VirtualThreadsLab.java` as provided, with TODO 5.1 open.

<details>
<summary>Full <code>VirtualThreadsLab.java</code></summary>

```java
package com.helpdeskpro.lab.virtualthreads;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Virtual threads.
 *
 * Each task below simulates a blocking call, such as a JDBC query that takes
 * {@code work} to return. The experiment compares how long N such tasks take
 * on a small fixed pool of platform threads and on virtual threads.
 *
 * In a Spring Boot application you get this behaviour for request handling by
 * setting {@code spring.threads.virtual.enabled=true}.
 */
public class VirtualThreadsLab {

    private static final int PLATFORM_POOL_SIZE = 20;

    public static void main(String[] args) {
        int tasks = 500;
        Duration work = Duration.ofMillis(100);

        System.out.println("Tasks: " + tasks + ", each blocks for " + work.toMillis() + " ms");
        System.out.println("Platform threads (pool of " + PLATFORM_POOL_SIZE + "): "
                + runWithPlatformThreads(tasks, work) + " ms");

        try {
            System.out.println("Virtual threads: " + runWithVirtualThreads(tasks, work) + " ms");
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }

        showThreadNames();
    }

    // Provided: a fixed pool of platform threads.
    public static long runWithPlatformThreads(int tasks, Duration work) {
        long start = System.nanoTime();
        try (var executor = Executors.newFixedThreadPool(PLATFORM_POOL_SIZE)) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    Thread.sleep(work);
                    return null;
                });
            }
        }
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }

    // ---------------------------------------------------------------------
    // TODO 5.1: run the same tasks on virtual threads
    //
    // Run `tasks` tasks that each block for `work`, using one virtual thread
    // per task, and return the elapsed time in milliseconds.
    //
    // Rules:
    //  - the method must not return until every task has finished
    //  - do not set a pool size; there is no pool to tune
    //  - the structure should look very close to runWithPlatformThreads
    //
    // Hints: Executors.newVirtualThreadPerTaskExecutor(), try-with-resources
    // ---------------------------------------------------------------------
    public static long runWithVirtualThreads(int tasks, Duration work) {
        throw new UnsupportedOperationException("TODO 5.1: implement runWithVirtualThreads");
    }

    // Provided: look at the thread names. Compare them with a platform thread.
    static void showThreadNames() {
        Thread platform = new Thread(
                () -> System.out.println("platform: " + Thread.currentThread()), "platform-demo");
        platform.start();
        Thread virtual = Thread.ofVirtual().name("virtual-demo").unstarted(
                () -> System.out.println("virtual:  " + Thread.currentThread()
                        + " isVirtual=" + Thread.currentThread().isVirtual()));
        virtual.start();
        try {
            platform.join();
            virtual.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

</details>

This matches [VirtualThreadsLab.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/virtualthreads/VirtualThreadsLab.java) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The test fails with "tasks must actually have run" and a time near 0 ms | The method returns before the tasks finish | Make sure the executor is closed (waited on) before you read the elapsed time |
| `unreported exception java.lang.InterruptedException` | The task lambda is treated as a `Runnable` | Have the lambda return a value, as the provided platform version does |
| The test fails with a time near 5,000 ms | A fixed-size pool was used | Create the executor that starts one virtual thread per task |
| The test fails only when the machine is busy | Another process is using the CPU during the run | Re-run it with other heavy applications closed |

Next: **[Step 8 — Recap and exercises](08-recap-and-exercises.md)**.
