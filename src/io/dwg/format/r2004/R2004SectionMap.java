package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.format.common.SectionDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * §4 Section Map 페이지 파싱. 섹션 이름과 페이지 목록 구성.
 */
public class R2004SectionMap {
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2004SectionMap() {}

    public static R2004SectionMap read(BitInput input, long pageMapOffset) throws Exception {
        R2004SectionMap map = new R2004SectionMap();

        // pageMapOffset 위치로 이동
        input.seek(pageMapOffset * 8);

        // 섹션 수 (RL)
        int sectionCount = input.readRawLong();

        for (int i = 0; i < sectionCount; i++) {
            try {
                SectionDescriptor desc = R2004DataSectionDescriptor.read(input);
                map.descriptors.add(desc);
            } catch (Exception e) {
                break;
            }
        }

        return map;
    }

    public List<SectionDescriptor> descriptors() {
        return descriptors;
    }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }
}
