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
