package io.dwg.sections.handles;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.io.ByteBufferBitOutput;
import io.dwg.core.io.SectionOutputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.SectionWriter;

import java.util.List;

/**
 * Handles Section Writer (Spec §23: AcDb:Handles)
 * Serializes handle-to-offset mapping for object reference resolution
 */
public class HandlesSectionWriter implements SectionWriter<HandleRegistry> {

    private static final int BLOCK_SIZE = 2048; // Default block size for handles

    @Override
    public String sectionName() {
        return SectionType.HANDLES.sectionName();
    }

    @Override
    public SectionOutputStream write(HandleRegistry registry, DwgVersion version) throws Exception {
        SectionOutputStream section = new SectionOutputStream(sectionName());
        ByteBufferBitOutput output = new ByteBufferBitOutput();

        // Get sorted entries for deterministic output
        List<HandleEntry> entries = registry.sortedEntries();

        if (entries.isEmpty()) {
            // Empty handles section - just write terminator
            output.writeRawShort((short) 2);
            return section;
        }

        // Write handles in blocks with deltas
        int blockStart = 0;
        while (blockStart < entries.size()) {
            int blockEnd = Math.min(blockStart + BLOCK_SIZE, entries.size());
            writeBlock(output, entries.subList(blockStart, blockEnd));
            blockStart = blockEnd;
        }

        // Write terminator block (size = 2)
        output.writeRawShort((short) 2);

        // Copy the output to section
        byte[] data = output.toByteArray();
        for (byte b : data) {
            section.getBitOutput().writeRawChar(b & 0xFF);
        }

        return section;
    }

    private void writeBlock(ByteBufferBitOutput blockOutput, List<HandleEntry> entries) throws Exception {
        ByteBufferBitOutput blockData = new ByteBufferBitOutput();
        BitStreamWriter writer = new BitStreamWriter(blockData, DwgVersion.R2004);

        long lastHandle = 0;
        long lastOffset = 0;

        for (HandleEntry entry : entries) {
            long handleDelta = entry.handle() - lastHandle;
            long offsetDelta = entry.offset() - lastOffset;

            writer.writeModularChar((int) handleDelta);
            writer.writeModularChar((int) offsetDelta);

            lastHandle = entry.handle();
            lastOffset = entry.offset();
        }

        // Write end marker
        writer.writeModularChar(0);

        byte[] data = blockData.toByteArray();
        int blockSize = data.length + 4; // data + size(2) + crc(2)

        // Write block size
        blockOutput.writeRawShort((short) blockSize);

        // Write block data
        for (byte b : data) {
            blockOutput.writeRawChar(b & 0xFF);
        }

        // Write CRC (simplified - proper implementation would calculate actual CRC)
        blockOutput.writeRawShort((short) 0);
    }
}
