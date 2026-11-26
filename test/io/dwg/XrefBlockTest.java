package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.concrete.DwgBlockHeader;

/** An R2013 xref BLOCK_HEADER has no owned object count. */
class XrefBlockTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void xrefBlockOfR2013() throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path("2013/gh44-error.dwg")));
        for (long handle : new long[] {0xBA16L, 0xBA6AL}) {
            DwgBlockHeader b = (DwgBlockHeader) doc.objectMap().get(handle);
            assertTrue(b.isXref());
            assertTrue(b.xrefPath().endsWith(".dwg"), b.xrefPath());
            assertEquals(1, b.insertCount());
            assertEquals(4, b.insertUnits());
            assertTrue(doc.issues().stream().noneMatch(i -> i.handle() == handle));
        }
    }
}
