package info.kgeorgiy.ja.boin.iterative;

import info.kgeorgiy.java.advanced.iterative.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Methods for some parallel operations
 */
public class IterativeParallelism implements ScalarIP {

    private final ParallelMapper mapper;

    /**
     * Creates new instance of IterativeParallelism that will use
     * given mapper to do parallel tasks
     *
     * @param mapper given mapper
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * New instance of Iterative parallelism without mapper
     */
    public IterativeParallelism() {
        mapper = null;
    }

    /**
     * Class representing simple interval
     *
     * @param start interval start
     * @param end   interval end >= start, exclusive
     */
    record Interval(int start, int end) {
        Stream<Integer> stream() {
            return IntStream.range(start, end).boxed();
        }
    }

    // ---parallel utils---

    private Integer parallelOperation(int threads, List<?> list, Function<Stream<Integer>, Integer> op, Predicate<Integer> breakCond) throws InterruptedException {
        return parallelOperation(threads, list, op, op);
    }

    private Integer parallelOperation(int threads, List<?> list, Function<Stream<Integer>, Integer> op) throws InterruptedException {
        return parallelOperation(threads, list, op, op);
    }

    private <T extends Number> T parallelOperation(int threads, List<?> list, Function<Stream<Integer>, T> op, Function<Stream<T>, T> finalizer) throws InterruptedException {
        if (list.isEmpty()) {
            throw new NoSuchElementException("Empty list");
        } else if (threads <= 0) {
            throw new IllegalArgumentException("Threads less than zero");
        }

        int step = (list.size() + threads - 1) / threads;

        List<T> results;
        if (mapper != null) {
            List<Stream<Integer>> intervals = IntStream.range(0, threads).mapToObj(i -> new Interval(i * step, Math.min((i + 1) * step, list.size())).stream()).toList();
            results = mapper.map(op, intervals);
        } else {
            results = new ArrayList<>(Collections.nCopies(threads, null));
            Thread[] threadList = new Thread[threads];
            IntStream.range(0, threads).forEach(i -> {
                        Interval interval = new Interval(i * step, Math.min((i + 1) * step, list.size()));
                        threadList[i] = new Thread(() -> results.set(i, op.apply(interval.stream())));
                    }
            );
            Arrays.stream(threadList).forEach(Thread::start);
            joinThreads(Arrays.stream(threadList).toList());
        }

        return finalizer.apply(results.stream());
    }

    public static void joinThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException exceptions = new InterruptedException();
        boolean wasInterrupted = false;
        for (Thread thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    exceptions.addSuppressed(e);
                    wasInterrupted = true;
                }
            }
        }
        if (wasInterrupted) {
            throw exceptions;
        }
    }
    private Integer parallelReduce(int threads, List<?> list, BinaryOperator<Integer> f, Integer defaultVal) throws InterruptedException {
        return parallelOperation(threads, list, (s) -> s.reduce(defaultVal, f));
    }

    private <T> Stream<Integer> filterIndexes(Stream<Integer> stream, List<T> list, Predicate<? super T> predicate) {
        return stream.filter(i -> i >= 0 && predicate.test(list.get(i)));
    }

    private <T> Comparator<Integer> compareIndexes(List<T> list, Comparator<? super T> comparator) {
        return Comparator.comparing(list::get, comparator); //.thenComparing((i1, i2) -> -i1.compareTo(i2));
    }

    // ---ScalarIp methods---

    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return parallelReduce(threads, list, BinaryOperator.maxBy(compareIndexes(list, comparator)), 0);
    }

    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return parallelReduce(threads, list, BinaryOperator.minBy(compareIndexes(list, comparator)), 0);
    }

    private <T> int indexImpl(int threads, List<T> values, Predicate<? super T> predicate, Function<Stream<Integer>, Optional<Integer>> f) throws InterruptedException {
        return parallelOperation(threads, values, s -> f.apply(filterIndexes(s, values, predicate)).orElse(-1));
    }

    private <T, R extends Number> R indexImpl(int threads, List<T> values, Predicate<? super T> predicate, Function<Stream<Integer>, R> f, Function<Stream<R>, R> finalizer) throws InterruptedException {
        return parallelOperation(threads, values, s -> f.apply(filterIndexes(s, values, predicate)), finalizer);
    }

    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return indexImpl(threads, values, predicate, Stream::findFirst);
    }

    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return indexImpl(threads, values, predicate, s -> s.reduce((a, b) -> b));
    }

    @Override
    public <T> long sumIndices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return indexImpl(threads, values, predicate, s -> s.mapToLong(i -> (long) i).sum(), s -> s.reduce(0L, Long::sum));
    }
}

