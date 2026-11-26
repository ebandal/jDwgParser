package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.AbstractDwgAcisEntity;

/** The ACIS models of REGION and 3DSOLID are kept as SAT text or SAB bytes. */
class AcisTest {

    /** The REGIONs and the 3DSOLID of example_*.dwg. */
    private static final long[] EXAMPLE_HANDLES = {0x176, 0x2E1, 0x37D};

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void r2000SolidHoldsSatText() throws Exception {
        List<AbstractDwgAcisEntity> solids = acis("2000/Cone.dwg");
        assertEquals(1, solids.size());
        String sat = solids.get(0).satText();
        assertNotNull(sat);
        assertTrue(sat.startsWith("400 "), sat.substring(0, 20));
        assertTrue(sat.contains("Autodesk AutoCAD"));
        assertEquals(4, solids.get(0).wires().size());
    }

    @Test
    void r2004RegionHoldsSabBytes() throws Exception {
        for (AbstractDwgAcisEntity e : acis("example_2004.dwg")) {
            byte[] sab = e.sabData();
            assertNotNull(sab);
            String text = new String(sab, StandardCharsets.ISO_8859_1);
            assertTrue(text.startsWith("ACIS BinaryFile"));
            assertTrue(text.endsWith("End\u000E\u0002of\u000E\u0004ACIS\r\u0004data"));
        }
    }

    /** R13 lacks one count after the isolines; its SAT text is that of the R14 copy. */
    @Test
    void r13AndR14HoldSatText() throws Exception {
        for (String file : new String[] {"example_r13.dwg", "example_r14.dwg"}) {
            DwgDocument doc = open(file);
            for (long h : EXAMPLE_HANDLES) {
                AbstractDwgAcisEntity e = (AbstractDwgAcisEntity) doc.objectMap().get(h);
                assertNotNull(e.satText(), file + " " + Long.toHexString(h));
                assertTrue(e.satText().contains("body"), file + " " + Long.toHexString(h));
                assertEquals(4, e.isolineCount(), file);
            }
        }
    }

    /** From R2013 the model is in the data storage section, under the entity's handle. */
    @Test
    void r2013ModelComesFromTheDataStorage() throws Exception {
        for (String file : new String[] {"example_2013.dwg", "example_2018.dwg"}) {
            DwgDocument doc = open(file);
            for (long h : EXAMPLE_HANDLES) {
                AbstractDwgAcisEntity e = (AbstractDwgAcisEntity) doc.objectMap().get(h);
                assertTrue(e.isEmpty(), file);
                assertNotNull(e.sabData(), file + " " + Long.toHexString(h));
                String head = new String(e.sabData(), 0, 16, StandardCharsets.ISO_8859_1);
                assertTrue(head.contains("BinaryFile"), file + " " + head);
                assertNotNull(e.guid(), file);
                assertEquals(4, e.isolineCount(), file);
            }
            assertEquals(new UUID(0, 0), ((AbstractDwgAcisEntity) doc.objectMap().get(0x176L)).guid());
        }
    }

    private static DwgDocument open(String file) throws Exception {
        return DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
    }

    private static List<AbstractDwgAcisEntity> acis(String file) throws Exception {
        DwgDocument doc = open(file);
        return doc.objectMap().values().stream().filter(o -> o instanceof AbstractDwgAcisEntity)
                .map(o -> (AbstractDwgAcisEntity) o).collect(Collectors.toList());
    }
}
