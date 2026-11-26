package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.type.DwgHandleRef;

/**
 * The header variables (spec chapter 9) of the copies of the same drawing read the same in every
 * version, their streams end where they should, and their handles are objects of the file.
 */
class HeaderTest {

    private static final String[] FILES = {
        "example_r14.dwg", "example_2000.dwg", "example_2004.dwg", "example_2007.dwg",
        "example_2010.dwg", "example_2013.dwg", "example_2018.dwg",
    };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void headerReadsTheSameInEveryVersion() throws Exception {
        for (String file : FILES) {
            DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
            assertTrue(doc.issues().stream().noneMatch(i -> String.valueOf(i).contains("AcDb:Header")),
                    file + " " + doc.issues());
            Map<String, Object> h = doc.header().toMap();
            assertEquals(new DwgHandleRef(0x1F), h.get("BLOCK_RECORD_MODEL_SPACE"), file);
            assertEquals(new DwgHandleRef(0x8A), h.get("CLAYER"), file);
            assertEquals(new DwgHandleRef(0xC), h.get("DICTIONARY_NAMED_OBJECTS"), file);
            assertEquals(2.5, (Double) h.get("DIMTXT"), 1e-9, file);
            assertEquals(20.0, (Double) h.get("CMLSCALE"), 1e-9, file);
            assertEquals(2454762, h.get("TDCREATE_DAY"), file);
            for (Map.Entry<String, Object> e : h.entrySet()) {
                if (e.getValue() instanceof DwgHandleRef && !e.getKey().equals("HANDSEED")) {
                    long handle = ((DwgHandleRef) e.getValue()).rawHandle();
                    assertTrue(handle == 0 || doc.objectMap().containsKey(handle), file + " " + e);
                }
            }
        }
    }
}
