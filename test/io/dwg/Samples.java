package io.dwg;

import java.io.File;

import org.junit.jupiter.api.Assumptions;

/**
 * Where the sample drawings are. They are not part of the repository; keep them in a local
 * folder and point to it with the system property {@code dwg.samples} or the environment
 * variable {@code DWG_SAMPLES}. Without either, the folder {@code samples} in the working
 * directory is used. When the folder is missing the tests that need it are skipped.
 */
final class Samples {

    static final File DIR = locate();

    private Samples() {
    }

    private static File locate() {
        String dir = System.getProperty("dwg.samples");
        if (dir == null || dir.isEmpty()) {
            dir = System.getenv("DWG_SAMPLES");
        }
        return new File(dir == null || dir.isEmpty() ? "samples" : dir);
    }

    /** The path of a sample file or folder, relative to the samples folder. */
    static String path(String relative) {
        return new File(DIR, relative).getPath();
    }

    /** Skips the calling tests when the samples folder is not there. */
    static void assumePresent() {
        Assumptions.assumeTrue(DIR.isDirectory(),
                "sample drawings not found at " + DIR.getAbsolutePath() + " (set -Ddwg.samples or DWG_SAMPLES)");
    }
}
