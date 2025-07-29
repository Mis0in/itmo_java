package info.kgeorgiy.ja.boin.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Work that will be done in a single thread
 */
class Work<R> {
    private final Supplier<R> task;
    private boolean isDone;
    private Exception e;
    private R workRes = null;

    Work(Supplier<R> task) {
        this.task = task;
        isDone = false;
        e = null;
    }

    synchronized void doWork() {
        try {
            workRes = task.get();
        } catch (Exception e) {
            this.e = e;
        }
        isDone = true;
        notify();
    }

    synchronized void waitDone() throws InterruptedException {
        while (!isDone) {
            wait();
        }
    }

    Exception getErr() {
        return e;
    }

    R getResult() {
        return workRes;
    }
}

/**
 * Parallel queue for works
 */
class WorksQueue {
    private final Deque<Work<?>> worksOrder = new ArrayDeque<>();

    public synchronized void add(Work<?> w) {
        worksOrder.add(w);
        notify();
    }

    public synchronized Work<?> take() throws InterruptedException {
        while (worksOrder.isEmpty()) {
            wait();
        }
        return worksOrder.pop();
    }
}

/**
 * Maps function over lists in parallel
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final WorksQueue queue = new WorksQueue();

    /**
     * Creates new instance of class
     * Starts {@code threads} that waits for map call
     *
     * @param threads number of threads that will be used to parallel map
     */
    public ParallelMapperImpl(int threads) {
        workers = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Work<?> task = queue.take();
                        task.doWork();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        workers.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>();
        List<Work<R>> currentWorks = new ArrayList<>();

        // :NOTE: save R in Work
        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            Work<R> w = new Work<>(() -> f.apply(args.get(finalI)));
            currentWorks.add(w);
            queue.add(w);
        }

        InterruptedException e = new InterruptedException();
        currentWorks.forEach(work -> {
            try {
                work.waitDone();
                Exception err = work.getErr();
                if (err != null) {
                    e.addSuppressed(err);
                } else {
                    result.add(work.getResult());
                }
            } catch (InterruptedException ignored) {
            }
        });

        if (e.getSuppressed().length > 0) {
            throw e;
        }
        return result;
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt); // :NOTE: copypaste

        try {
            IterativeParallelism.joinThreads(workers);
        } catch (InterruptedException ignored) {
        }
    }
}
