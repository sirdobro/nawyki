package cc.intmain.nawyki.application;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Two independent tasks, no shared transaction or mutable result accumulator. */
public final class ParallelReport {
    private ParallelReport() {}
    public static <A, B, R> R combine(ExecutorService executor, Supplier<A> first, Supplier<B> second,
                                     BiFunction<A, B, R> merge) {
        Future<A> a = executor.submit(first::get);
        Future<B> b;
        try { b = executor.submit(second::get); }
        catch (RejectedExecutionException overloaded) { a.cancel(true); throw overloaded; }
        try { return merge.apply(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Report interrupted", interrupted);
        } catch (ExecutionException | TimeoutException failed) {
            throw new IllegalStateException("Report unavailable", failed);
        } finally { a.cancel(true); b.cancel(true); }
    }
}
