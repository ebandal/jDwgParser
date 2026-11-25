package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgReader;
import io.dwg.core.type.Point3D;
import io.dwg.entities.concrete.DwgMLeader;

/** The MULTILEADER of Leader.dwg reads the same in every version. */
class MLeaderTest {

    private static final String[] VERSIONS = { "2000", "2004", "2007", "2010", "2013", "2018" };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void leaderReadsTheSameInEveryVersion() throws Exception {
        for (String version : VERSIONS) {
            DwgMLeader m = DwgReader.defaultReader().open(Paths.get(Samples.path(version + "/Leader.dwg")))
                    .objectMap().values().stream().filter(o -> o instanceof DwgMLeader).map(o -> (DwgMLeader) o)
                    .findFirst().orElse(null);
            assertNotNull(m, version);
            assertEquals("LEADER", m.text(), version);
            assertEquals(1, m.leaderRoots().size(), version);
            Point3D connection = (Point3D) m.leaderRoots().get(0).get("CONNECTION");
            assertEquals(18.21, connection.x(), 0.01, version);
            assertEquals(14.54, connection.y(), 0.01, version);
        }
    }
}
