package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgReader;
import io.dwg.entities.concrete.DwgTable;

/** The table of example_*.dwg reads the same in both formats (until R2007 and from R2010). */
class TableTest {

    private static final String[] FILES = {
        "example_2000.dwg", "example_2004.dwg", "example_2007.dwg", "example_2010.dwg",
        "example_2013.dwg", "example_2018.dwg",
    };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void tableReadsTheSameInEveryVersion() throws Exception {
        for (String file : FILES) {
            DwgTable t = DwgReader.defaultReader().open(Paths.get(Samples.path(file))).objectMap().values()
                    .stream().filter(o -> o instanceof DwgTable).map(o -> (DwgTable) o).findFirst().orElse(null);
            assertNotNull(t, file);
            assertEquals("test", t.cellTexts().get(0).get(0), file);
            assertFalse(t.blockHeaderHandle().isNull(), file);
            assertEquals(t.cellTexts().size(), t.rowCount(), file);
        }
    }
}
