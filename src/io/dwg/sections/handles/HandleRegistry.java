package io.dwg.sections.handles;

import java.util.*;

/**
 * 핸들 값 → 파일 오프셋 매핑. 모든 객체 조회의 중심.
 * <p>Kept as two arrays of primitive longs, sorted by handle when first looked up (a map of
 * boxed longs took about 80 bytes per object). A handle put twice keeps its last offset.
 */
public class HandleRegistry {
    private long[] handles = new long[64];
    private long[] offsets = new long[64];
    private int size;
    private boolean sorted = true;

    public void put(long handle, long offset) {
        if (size == handles.length) {
            handles = Arrays.copyOf(handles, Math.max(16, size * 2));
            offsets = Arrays.copyOf(offsets, Math.max(16, size * 2));
        }
        if (size > 0 && handle <= handles[size - 1]) {
            sorted = false;
        }
        handles[size] = handle;
        offsets[size] = offset;
        size++;
    }

    public Optional<Long> offsetFor(long handle) {
        sort();
        int i = Arrays.binarySearch(handles, 0, size, handle);
        return i >= 0 ? Optional.of(offsets[i]) : Optional.empty();
    }

    public int size() {
        sort();
        return size;
    }

    public Set<Long> allHandles() {
        sort();
        Set<Long> set = new LinkedHashSet<>(size * 2);
        for (int i = 0; i < size; i++) {
            set.add(handles[i]);
        }
        return Collections.unmodifiableSet(set);
    }

    public List<HandleEntry> sortedEntries() {
        sort();
        List<HandleEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new HandleEntry(handles[i], offsets[i]));
        }
        return list;
    }

    /** Sorts by handle; of equal handles the one put last stays. */
    private void sort() {
        if (sorted) {
            if (handles.length > size) {           // drop the room left by growing
                handles = Arrays.copyOf(handles, size);
                offsets = Arrays.copyOf(offsets, size);
            }
            return;
        }
        Integer[] order = new Integer[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        // stable: equal handles keep the order they were put in
        Arrays.sort(order, Comparator.comparingLong(i -> handles[i]));
        long[] h = new long[size];
        long[] o = new long[size];
        int n = 0;
        for (int k = 0; k < size; k++) {
            int i = order[k];
            if (n > 0 && h[n - 1] == handles[i]) {
                o[n - 1] = offsets[i];              // the later put wins
            } else {
                h[n] = handles[i];
                o[n] = offsets[i];
                n++;
            }
        }
        handles = h;
        offsets = o;
        size = n;
        sorted = true;
    }
}
