package io.dwg;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgReadException;
import io.dwg.api.DwgReader;
import io.dwg.core.io.ReadLimits;

/**
 * Damaged files: the reader returns a document (with issues) or throws {@link DwgReadException},
 * never another exception, and it keeps to its limits.
 */
class RobustnessTest {

    private static final String[] FILES = {
        "example_r14.dwg", "example_2000.dwg", "example_2004.dwg", "example_2007.dwg",
        "example_2010.dwg", "example_2013.dwg", "example_2018.dwg",
    };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    /** Changed copies of the samples, from a fixed seed (the tool ReadFuzz does many more). */
    @Test
    void damagedFilesOpenOrThrowDwgReadException() throws Exception {
        Random random = new Random(7);
        for (String file : FILES) {
            byte[] original = Files.readAllBytes(Paths.get(Samples.path(file)));
            for (int run = 0; run < 40; run++) {
                byte[] d = original.clone();
                switch (run % 4) {
                    case 0: for (int i = 0; i < 10; i++) d[random.nextInt(d.length)] ^= (byte) (1 << random.nextInt(8)); break;
                    case 1: { int at = random.nextInt(d.length); Arrays.fill(d, at, Math.min(d.length, at + 200), (byte) 0xFF); break; }
                    case 2: { int at = random.nextInt(d.length); for (int i = at; i < Math.min(d.length, at + 64); i++) d[i] = (byte) random.nextInt(256); break; }
                    default: d = Arrays.copyOf(d, 6 + random.nextInt(d.length - 6));
                }
                try {
                    DwgReader.defaultReader().open(d);
                } catch (DwgReadException expected) {
                    // a file that cannot be read
                } catch (Throwable t) {
                    fail(file + " run " + run + ": " + t, t);
                }
            }
        }
    }

    @Test
    void readStopsAtItsByteLimit() {
        DwgReadException e = assertThrows(DwgReadException.class,
                () -> DwgReader.defaultReader().maxBytes(1000).open(Paths.get(Samples.path("example_2007.dwg"))));
        assertTrue(e.getMessage().contains("limit"), e.getMessage());
    }

    @Test
    void readStopsAtItsTimeLimit() {
        ReadLimits.begin(Long.MAX_VALUE, 1);
        try {
            long start = System.nanoTime();
            while (System.nanoTime() - start < 5_000_000L) {
                Thread.onSpinWait();
            }
            assertThrows(ReadLimits.LimitExceededException.class, ReadLimits::checkTime);
        } finally {
            ReadLimits.end();
        }
    }
}
