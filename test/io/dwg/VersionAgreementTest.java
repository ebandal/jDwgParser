package io.dwg;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.render.tool.VersionCompare;

/**
 * The samples hold the same drawings saved in several DWG versions. R2000 is verified against the reference
 * reader, so a file of another version is right when it renders like its R2000 counterpart.
 */
class VersionAgreementTest {
    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    private static final File SAMPLES = Samples.DIR;

    /** files whose content differs from the R2000 file, not only their version */
    private static final Set<String> DIFFERENT_CONTENT = Set.of(
            "2010/Leader.dwg");     // has one leader more than the other versions

    @Test
    void r14() {
        check("r14");
    }

    @Test
    void r2004() {
        check("2004");
    }

    @Test
    void r2007() {
        check("2007");
    }

    @Test
    void r2010() {
        check("2010");
    }

    @Test
    void r2013() {
        check("2013");
    }

    @Test
    void r2018() {
        check("2018");
    }

    /** R13 has no LWPOLYLINE and stores hatches as single lines, so a few pixels of anti-aliasing differ. */
    @Test
    void r13() {
        List<VersionCompare.Result> results = VersionCompare.compareVersion(SAMPLES, "r13");
        assertTrue(!results.isEmpty(), "no R13 sample");
        for (VersionCompare.Result r : results) {
            assertNull(r.error, r.name);
            assertTrue(r.differentPixels >= 0 && r.differentPixels <= r.pixels / 10_000,
                    "r13/" + r.name + " : " + r.differentPixels + " of " + r.pixels + " pixels differ " + r.differences);
        }
    }

    private static void check(String version) {
        List<VersionCompare.Result> results = VersionCompare.compareVersion(SAMPLES, version);
        assertTrue(results.size() >= 3, "too few " + version + " samples : " + results.size());
        for (VersionCompare.Result r : results) {
            assertNull(r.error, version + "/" + r.name);
            if (DIFFERENT_CONTENT.contains(version + "/" + r.name)) {
                continue;
            }
            assertTrue(r.identical(), version + "/" + r.name + " renders differently from R2000 ("
                    + r.differentPixels + " pixels) " + r.differences);
        }
    }
}
