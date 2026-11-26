package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.DwgObject;

/** {@code entities()} on drawings that have entities of types not supported. */
class DocumentEntitiesTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void entitiesLeavesOutTheGenericOnes() throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path("example_2000.dwg")));
        long typed = doc.objects(DwgObject.class).stream().filter(o -> o instanceof DwgEntity).count();
        long generic = doc.objects(DwgObject.class).stream().filter(o -> o.isEntity() && !(o instanceof DwgEntity)).count();
        assertTrue(generic > 0, "the sample should have entities of unsupported types");
        assertEquals(typed, doc.entities().size());
    }
}
