package io.dwg.sections.handles;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HandleRegistry의 래퍼. 범위 조회 기능 추가.
 */
public class ObjectOffsetMap {
    private final HandleRegistry registry;

    public ObjectOffsetMap(HandleRegistry registry) {
        this.registry = registry;
    }

    public Long offsetFor(long handle) {
        return registry.offsetFor(handle).orElse(null);
    }

    public List<Long> handlesInRange(long startHandle, long endHandle) {
        return registry.allHandles().stream()
            .filter(h -> h >= startHandle && h <= endHandle)
            .sorted()
            .collect(Collectors.toList());
    }
}
