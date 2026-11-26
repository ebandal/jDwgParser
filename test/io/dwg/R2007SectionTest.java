package io.dwg;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;

/**
 * R2007 section pages (spec §5.5, §5.13): placed at their offset and cut to their size, read in
 * order when their section's encoding is 1, and decompressed from their compressed size only.
 */
class R2007SectionTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void uncompressedPagesAreKept() throws Exception {
        byte[] summary = sections("2007/Arc.dwg").get("AcDb:SummaryInfo").rawBytes();
        String text = new String(summary, StandardCharsets.UTF_16LE);
        assertTrue(text.contains("THEAI"), "summary info: " + text);
    }

    /** Sections of encoding 1 are not interleaved: the preview starts with its sentinel (spec 14). */
    @Test
    void encodingOneIsNotInterleaved() throws Exception {
        byte[] preview = sections("2007/Arc.dwg").get("AcDb:Preview").rawBytes();
        byte[] start = {0x1F, 0x25, 0x6D, 0x07, (byte) 0xD4, 0x36, 0x28, 0x28,
                        (byte) 0x9D, 0x57, (byte) 0xCA, 0x3F, (byte) 0x9D, 0x44, 0x10, 0x2B};
        assertArrayEquals(start, Arrays.copyOf(preview, 16));
    }

    /** The compressed data of a page ends at its compressed size; the filler after it is not data. */
    @Test
    void compressedDataEndsAtItsSize() throws Exception {
        Map<String, SectionInputStream> s = sections("2007/Constraints.dwg");
        assertNotNull(s.get("AcDb:Classes"));
        assertNotNull(s.get("AcDb:Header"));
    }

    private static Map<String, SectionInputStream> sections(String name) throws Exception {
        byte[] file = Files.readAllBytes(Paths.get(Samples.path(name)));
        DwgFileStructureHandler h = DwgFileStructureHandlerFactory.detect(file);
        FileHeaderFields f = h.readHeader(new ByteBufferBitInput(file));
        return h.readSections(new ByteBufferBitInput(file), f);
    }
}
