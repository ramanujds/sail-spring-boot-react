package com.helpdeskpro.lab.virtualthreads;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class VirtualThreadsLabTest {

    // TODO 5.1
    // 1,000 tasks of 100 ms on 20 platform threads take about 5 seconds.
    // One virtual thread per task finishes in a fraction of a second.
    @Test
    void virtualThreadsFinishBlockingTasksFarFasterThanASmallPlatformPool() {
        long elapsedMillis = VirtualThreadsLab.runWithVirtualThreads(1_000, Duration.ofMillis(100));

        assertTrue(elapsedMillis >= 100, "tasks must actually have run, took " + elapsedMillis + " ms");
        assertTrue(elapsedMillis < 2_000, "expected well under 2 seconds, took " + elapsedMillis + " ms");
    }
}
