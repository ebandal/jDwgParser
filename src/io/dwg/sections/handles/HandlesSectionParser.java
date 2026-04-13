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
        BitInput input = stream.getBitInput();

        // 각 블록: RS 크기 → 데이터 → CRC(RS)
        while (!input.isEof()) {
            int blockSize = input.readRawShort() & 0xFFFF;
            if (blockSize == 2) break; // 종료 블록

            byte[] blockData = new byte[blockSize - 2];
            for (int i = 0; i < blockData.length; i++) {
                blockData[i] = (byte) input.readRawChar();
            }
            // CRC (RS) – 검증 생략
            input.readRawShort();

            parseBlock(blockData, registry);
        }

        return registry;
    }

    private void parseBlock(byte[] data, HandleRegistry registry) {
        io.dwg.core.io.ByteBufferBitInput blockInput =
            new io.dwg.core.io.ByteBufferBitInput(data);
        BitStreamReader r = new BitStreamReader(blockInput, DwgVersion.R2004);

        long lastHandle = 0;
        long lastOffset = 0;

        while (!blockInput.isEof()) {
            int handleDelta = r.readModularChar();
            if (handleDelta == 0) break;
            int offsetDelta = r.readModularChar();

            lastHandle += handleDelta;
            lastOffset += offsetDelta;
            registry.put(lastHandle, lastOffset);
        }
    }

    @Override
    public String sectionName() {
        return SectionType.HANDLES.sectionName();
    }
}
