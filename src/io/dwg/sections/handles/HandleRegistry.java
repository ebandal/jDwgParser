package io.dwg.sections.handles;

import java.util.*;

/**
 * 핸들 값 → 파일 오프셋 매핑. 모든 객체 조회의 중심.
 */
public class HandleRegistry {
    private final Map<Long, Long> handleToOffset = new HashMap<>();

    public void put(long handle, long offset) {
        handleToOffset.put(handle, offset);
    }

    public Optional<Long> offsetFor(long handle) {
        return Optional.ofNullable(handleToOffset.get(handle));
    }

    public int size() {
        return handleToOffset.size();
    }

    public Set<Long> allHandles() {
        return Collections.unmodifiableSet(handleToOffset.keySet());
    }

    public List<HandleEntry> sortedEntries() {
        List<HandleEntry> list = new ArrayList<>();
        handleToOffset.forEach((h, o) -> list.add(new HandleEntry(h, o)));
        list.sort(Comparator.comparingLong(HandleEntry::handle));
        return list;
    }
}
