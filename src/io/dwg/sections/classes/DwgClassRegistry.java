package io.dwg.sections.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 커스텀 클래스 번호 → DwgClassDefinition 매핑.
 */
public class DwgClassRegistry {
    private final Map<Integer, DwgClassDefinition> byNumber = new HashMap<>();
    private final Map<String, DwgClassDefinition> byDxfName = new HashMap<>();

    public void register(DwgClassDefinition def) {
        byNumber.put(def.classNumber(), def);
        if (def.dxfRecordName() != null) {
            byDxfName.put(def.dxfRecordName(), def);
        }
    }

    public Optional<DwgClassDefinition> find(int classNumber) {
        return Optional.ofNullable(byNumber.get(classNumber));
    }

    public Optional<DwgClassDefinition> findByDxfName(String dxfName) {
        return Optional.ofNullable(byDxfName.get(dxfName));
    }
}
