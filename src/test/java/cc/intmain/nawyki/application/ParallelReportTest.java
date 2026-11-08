package cc.intmain.nawyki.application;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParallelReportTest {
    @Test void bothTasksMustBeRunningBeforeEitherMayFinish() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var barrier = new CyclicBarrier(2);
            assertEquals("habits:workouts", ParallelReport.combine(executor,
                    () -> arrive(barrier, "habits"), () -> arrive(barrier, "workouts"), (a, b) -> a + ":" + b));
        }
    }
    @Test void failedTaskCannotProducePartialSuccess() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            assertThrows(IllegalStateException.class, () -> ParallelReport.combine(executor,
                    () -> { throw new IllegalArgumentException("read failed"); }, () -> 2, Integer::sum));
        }
    }
    @Test void rejectedWorkIsReported() {
        var executor = Executors.newFixedThreadPool(2); executor.shutdown();
        assertThrows(RejectedExecutionException.class, () -> ParallelReport.combine(executor, () -> 1, () -> 2, Integer::sum));
    }
    @Test void interruptedCallerKeepsInterruptFlag() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            Thread.currentThread().interrupt();
            try {
                assertThrows(IllegalStateException.class, () -> ParallelReport.combine(executor, () -> {
                    try { new CountDownLatch(1).await(); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    return 1;
                }, () -> 2, Integer::sum));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }
    private static String arrive(CyclicBarrier barrier, String value) {
        try { barrier.await(5, TimeUnit.SECONDS); return value; }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
