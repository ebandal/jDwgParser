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
 * Debug: Extract actual section names
 */
public class DebugSectionNames {
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
        System.out.println("Debug: Section names from decompressed SectionMap");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Parse sections and extract names
        int offset = 0;
        int sectionNum = 0;

        while (offset + 64 <= sectionMapDecompressed.length) {
            // Read 8 RLL values
            long[] values = new long[8];
            int tempOffset = offset;
            for (int i = 0; i < 8; i++) {
                values[i] = readRLL(sectionMapDecompressed, tempOffset + i * 8);
            }

            long nameLength = values[4];

            if (nameLength < 0 || nameLength >= 48) {
                System.out.printf("Section %d: Invalid name_length %d, stopping\n", sectionNum, nameLength);
                break;
            }

            tempOffset += 64;

            // Extract name bytes
            byte[] nameBytes = new byte[(int) nameLength];
            if (tempOffset + (int) nameLength <= sectionMapDecompressed.length) {
                System.arraycopy(sectionMapDecompressed, tempOffset, nameBytes, 0, (int) nameLength);
            }

            System.out.printf("Section %d (offset 0x%04X):\n", sectionNum, offset);
            System.out.printf("  name_length: %d\n", nameLength);
            System.out.printf("  raw bytes (hex): ");
            for (byte b : nameBytes) {
                System.out.printf("%02X ", b & 0xFF);
            }
            System.out.println();

            // Try different decodings
            String utf8 = new String(nameBytes, "UTF-8");
            System.out.printf("  UTF-8: %s\n", utf8);

            // Try treating as wide characters (16-bit)
            if (nameLength % 2 == 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nameBytes.length; i += 2) {
                    int wchar = (nameBytes[i] & 0xFF) | ((nameBytes[i + 1] & 0xFF) << 8);
                    if (wchar >= 32 && wchar < 127) {
                        sb.append((char) wchar);
                    } else if (wchar != 0) {
                        sb.append(String.format("[%04X]", wchar));
                    }
                }
                System.out.printf("  UTF-16LE: %s\n", sb.toString());
            }

            // Show section info
            System.out.printf("  data_size: 0x%X, num_pages: %d, encrypted: %d\n",
                values[0], values[7], values[2]);
            System.out.println();

            offset += 64 + (int) nameLength;
            sectionNum++;

            if (offset >= sectionMapDecompressed.length) {
                break;
            }
        }

        System.out.printf("Total sections: %d\n", sectionNum);
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
