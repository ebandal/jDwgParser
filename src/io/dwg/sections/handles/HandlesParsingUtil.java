package io.dwg.sections.handles;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;

/**
 * Reads the object map (AcDb:Handles, spec chapter 23).
 *
 * <p>The map is a series of sections, each:
 * <pre>
 *   size   raw short, most significant byte first, counting itself and the pairs
 *   pairs  until the section's data ends:
 *            handle offset from the previous handle (modular char, unsigned)
 *            location offset from the previous location (modular char, may be negative)
 *   CRC    raw short
 * </pre>
 * The map ends with a section of size 2. R2004+ store the same data compressed (§23.2), with
 * locations relative to the start of AcDb:AcDbObjects.
 *
 * <p>The previous handle and location start from 0 in every section. The specification's
 * pseudo code sets them once before all sections, but the sample files only read correctly
 * when each section starts again from 0.
 */
public class HandlesParsingUtil {

    /** Sections longer than this are taken as corrupt (the specification cuts them at 2032). */
    private static final int MAX_SECTION_SIZE = 2040;

    /** Reads the object map of an R13/R14 file from {@code input}. */
    public static void parseHandlesBlocksR13(BitInput input, HandleRegistry registry) {
        parse(new BitStreamReader(input, DwgVersion.R13), registry);
    }

    /** Reads the decompressed object map of an R2000+ file. */
    public static void parseHandlesPagesR2000(BitStreamReader reader, HandleRegistry registry) {
        parse(reader, registry);
    }

    private static void parse(BitStreamReader r, HandleRegistry registry) {
        while (!r.isEof()) {
            long sectionStart = r.position();
            int size = r.readBigEndianShort();
            if (size <= 2 || size > MAX_SECTION_SIZE) {
                break;
            }
            long dataEnd = sectionStart + size * 8L;

            long handle = 0;
            long location = 0;
            while (r.position() < dataEnd && !r.isEof()) {
                int handleOffset = r.readUnsignedModularChar();
                if (handleOffset == 0 || r.position() > dataEnd) {
                    break;
                }
                handle += handleOffset;
                location += r.readModularChar();
                registry.put(handle, location);
            }

            r.seek(dataEnd);
            if (!r.isEof()) {
                r.readBigEndianShort(); // CRC
            }
        }
    }

    public static long accumulateOffset(long currentOffset, int delta) {
        return currentOffset + delta;
    }
}
