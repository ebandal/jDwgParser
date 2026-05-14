package io.dwg.sections.classes;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 스펙 §10 AcDb:Classes 섹션 파서.
 */
public class ClassesSectionParser extends AbstractSectionParser<List<DwgClassDefinition>> {

    static final byte[] START_SENTINEL = {
        (byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
        (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
        (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A
    };
    static final byte[] END_SENTINEL = {
        (byte)0x72, (byte)0x5E, (byte)0x3B, (byte)0x47, (byte)0x3B, (byte)0x56,
        (byte)0x07, (byte)0x3A, (byte)0x3F, (byte)0x23, (byte)0x0B, (byte)0xA0,
        (byte)0x18, (byte)0x30, (byte)0x49, (byte)0x75
    };

    @Override
    public List<DwgClassDefinition> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        List<DwgClassDefinition> classes = new ArrayList<>();
        BitStreamReader r = reader(stream, version);

        // 시작 Sentinel (16바이트) 읽기
        validateSentinel(r, START_SENTINEL);

        // 섹션 크기 (RL)
        int sectionSize = r.getInput().readRawLong();
        long startBit = r.position();

        long endBit = startBit + (long) sectionSize * 8;
        while (r.position() < endBit - END_SENTINEL.length * 8) {
            try {
                DwgClassDefinition def = parseOneClass(r, version);
                classes.add(def);
            } catch (Exception e) {
                break;
            }
        }

        // 종료 Sentinel 검증
        validateSentinel(r, END_SENTINEL);

        return classes;
    }

    private DwgClassDefinition parseOneClass(BitStreamReader r, DwgVersion v) {
        DwgClassDefinition def = new DwgClassDefinition();
        def.setClassNumber(r.readBitShort());
        def.setVersion(r.readBitShort());
        def.setApplicationName(r.readVariableText());
        def.setCppClassName(r.readVariableText());
        def.setDxfRecordName(r.readVariableText());
        def.setWasAZombie(r.getInput().readBit() == false ? false : true);
        def.setAnEntity(r.readBitShort() != 0);
        return def;
    }

    @Override
    public String sectionName() {
        return SectionType.CLASSES.sectionName();
    }
}
