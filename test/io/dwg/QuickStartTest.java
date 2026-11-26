package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLine;

/** The quick start of the README, as it is written there. */
class QuickStartTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void quickStartRuns() throws Exception {
        List<String> printed = new ArrayList<>();

        // --- README quick start ---
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path("example_2000.dwg")));
        for (DwgLine line : doc.objects(DwgLine.class)) {
            printed.add(line.start() + " -> " + line.end() + " on " + doc.layerOf(line).map(DwgLayer::name).orElse("?"));
        }
        doc.issues().forEach(issue -> printed.add(issue.toString()));
        // ---

        assertFalse(printed.isEmpty());
        assertTrue(printed.get(0).contains(" -> "), printed.get(0));
        assertTrue(printed.stream().noneMatch(p -> p.endsWith(" on ?")), "every line has its layer");
        assertEquals(doc.objects(DwgLine.class).size(), printed.size(), "no issues");
    }
}
