package io.dwg.sections.handles;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

/**
 * 스펙 §23 AcDb:Handles (Object Map) 섹션 파서.
 * 섹션을 RS 크기 단위 블록으로 읽어 handle↔offset 누적 합산.
 */
public class HandlesSectionParser extends AbstractSectionParser<HandleRegistry> {

    @Override
    public HandleRegistry parse(SectionInputStream stream, DwgVersion version) throws Exception {
        HandleRegistry registry = new HandleRegistry();

        if (version.until(io.dwg.core.version.DwgVersion.R14)) {
            BitInput input = stream.getBitInput();
            HandlesParsingUtil.parseHandlesBlocksR13(input, registry);
        } else {
            io.dwg.core.io.ByteBufferBitInput bitInput =
                new io.dwg.core.io.ByteBufferBitInput(stream.rawBytes());
            BitStreamReader reader = new BitStreamReader(bitInput, version);
            HandlesParsingUtil.parseHandlesPagesR2000(reader, registry);
        }

        return registry;
    }

    @Override
    public String sectionName() {
        return SectionType.HANDLES.sectionName();
    }
}
