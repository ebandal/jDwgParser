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

        System.out.printf("[DEBUG] Handles: Parsing section for version=%s\n", version);

        // All DWG versions use the same Handles format:
        // - RS_BE (big-endian) block/page sizes
        // - UMC handle_delta (unsigned)
        // - MC offset_delta (signed)
        // - Cumulative offset calculation
        if (version.until(io.dwg.core.version.DwgVersion.R14)) {
            // R13/R14: Block-based format
            BitInput input = stream.getBitInput();
            HandlesParsingUtil.parseHandlesBlocksR13(input, registry);
        } else {
            // R2000+: Page-based format (after LZ77 decompression in R2004+)
            io.dwg.core.io.ByteBufferBitInput bitInput =
                new io.dwg.core.io.ByteBufferBitInput(stream.rawBytes());
            BitStreamReader reader = new BitStreamReader(bitInput, version);
            HandlesParsingUtil.parseHandlesPagesR2000(reader, registry);
        }

        System.out.printf("[DEBUG] Handles: Total entries=%d\n", registry.allHandles().size());
        return registry;
    }

    @Override
    public String sectionName() {
        return SectionType.HANDLES.sectionName();
    }
}
