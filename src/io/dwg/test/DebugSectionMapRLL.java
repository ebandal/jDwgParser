package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Debug: Print raw RLL values from decompressed SectionMap
 */
public class DebugSectionMapRLL {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        // Read PageMap
        long pageMapFileOffset = 0x480L + header.pageMapOffset();
        byte[] pageMapDecompressed = R2007SystemPageReader.readSystemPage(
            input,
            pageMapFileOffset,
            header.pageMapSizeComp(),
            header.pageMapSizeUncomp(),
            header.pageMapCorrection()
        );

        List<R2007PageMapParser.PageMapEntry> pageMap =
            R2007PageMapParser.parsePageMap(pageMapDecompressed);

        // Find SectionMap page offset
        long cumulativeOffset = 0;
        long sectionMapFileOffset = -1;
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == header.sectionsMapId()) {
                sectionMapFileOffset = 0x480L + header.pageMapOffset() + cumulativeOffset;
                break;
            }
            cumulativeOffset += entry.size;
        }

        // Read SectionMap
        byte[] sectionMapDecompressed = R2007SystemPageReader.readSystemPage(
            input,
            sectionMapFileOffset,
            header.sectionsMapSizeComp(),
            header.sectionsMapSizeUncomp(),
            header.sectionsMapCorrection()
        );

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debug: Raw RLL values in decompressed SectionMap");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("Decompressed size: %d bytes\n\n", sectionMapDecompressed.length);

        // Read RLL values 8 per section
        int offset = 0;
        int sectionNum = 0;

        while (offset + 64 <= sectionMapDecompressed.length) {
            long[] values = new long[8];
            for (int i = 0; i < 8; i++) {
                values[i] = readRLL(sectionMapDecompressed, offset + i * 8);
            }

            System.out.printf("Section %d (offset 0x%04X):\n", sectionNum, offset);
            System.out.printf("  data_size:    0x%016X (%d)\n", values[0], values[0]);
            System.out.printf("  max_size:     0x%016X (%d)\n", values[1], values[1]);
            System.out.printf("  encrypted:    0x%016X (%d)\n", values[2], values[2]);
            System.out.printf("  hashcode:     0x%016X\n", values[3]);
            System.out.printf("  name_length:  0x%016X (%d)\n", values[4], values[4]);
            System.out.printf("  unknown:      0x%016X (%d)\n", values[5], values[5]);
            System.out.printf("  encoded:      0x%016X (%d)\n", values[6], values[6]);
            System.out.printf("  num_pages:    0x%016X (%d)\n", values[7], values[7]);

            // Sanity check
            if (values[4] < 0 || values[4] >= 48) {
                System.out.printf("  ⚠️ Invalid name_length: %d\n", values[4]);
                break;
            }

            System.out.println();
            offset += 64;
            sectionNum++;

            if (offset >= sectionMapDecompressed.length) {
                break;
            }
        }

        System.out.printf("Total sections read: %d\n", sectionNum);
        System.out.printf("Bytes remaining: %d\n", sectionMapDecompressed.length - offset);
    }

    private static long readRLL(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            return 0;
        }
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long)(data[offset + i] & 0xFF)) << (i * 8);
        }
        return value;
    }
}
