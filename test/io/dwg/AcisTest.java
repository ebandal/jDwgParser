package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.AbstractDwgAcisEntity;

/** The ACIS models of REGION and 3DSOLID are kept as SAT text or SAB bytes. */
class AcisTest {

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

    private static List<AbstractDwgAcisEntity> acis(String file) throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
        return doc.objectMap().values().stream().filter(o -> o instanceof AbstractDwgAcisEntity)
                .map(o -> (AbstractDwgAcisEntity) o).collect(Collectors.toList());
    }
}
