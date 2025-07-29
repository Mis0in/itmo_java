package info.kgeorgiy.ja.boin.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private final List<T> array;
    private final Comparator<? super T> comparator;
    private final Comparator<Object> DEFAULT_ORDER = (o1, o2) -> Collections.reverseOrder().reversed().compare(o1, o2);

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        // :NOTE:
        // new TreeSet<>();

        this.comparator = comparator;
        Set<T> set = new TreeSet<>(comparator);
        set.addAll(collection);
        array = List.copyOf(set);
    }

    // :NOTE: ArraySet(Comparator)
    public ArraySet(Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    private ArraySet(List<T> sortedArray, Comparator<T> comparator) {
        array = sortedArray;
        this.comparator = comparator;
    }

    public int compare(T first, T second) {
        return comparator == null ? DEFAULT_ORDER.compare(first, second) : comparator.compare(first, second);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    private int binarySearch(T element) {
        return Collections.binarySearch(array, element, this::compare);
    }

    private int findIndex(T element) {
        int result = binarySearch(element);
        return result < 0 ? -(result + 1) : result;
    }

    private SortedSet<T> indexSubSet(int find, int sind) {
        return new ArraySet<>(array.subList(find, sind), comparator);
    }

    @Override
    public SortedSet<T> subSet(T start, T end) {
        if (compare(start, end) > 0) {
            throw new IllegalArgumentException("Start index is bigger than end index");
        }
        return headSet(end).tailSet(start);
    }

    @Override
    public SortedSet<T> headSet(T end) {
        return indexSubSet(0, findIndex(end));
    }

    @Override
    public SortedSet<T> tailSet(T start) {
        return indexSubSet(findIndex(start), size());
    }

    @Override
    public T first() {
        return array.getFirst();
    }

    @Override
    public T last() {
        return array.getLast();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object elem) {
        return binarySearch((T) elem) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return array.iterator();
    }
}
