package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.dwg.render.tool.ReaderCheck;

/**
 * DwgReader must agree with the reference reader of io.dwg.render.r2000 on the R2000 samples :
 * every object under its handle, as the right class, with equal values and handles.
 */
class ReaderAgreementTest {

    @Test
    void r2000SamplesAgree() {
        List<File> files = ReaderCheck.collect("samples/2000", "samples/example_2000.dwg", "samples/sample_2000.dwg");
        assertTrue(files.size() >= 24, "R2000 samples are missing : found " + files.size());

        ReaderCheck.Result result = ReaderCheck.check(files);

        assertTrue(result.skipped.isEmpty(), "files not compared : " + result.skipped);
        assertEquals(result.objects, result.objectsFound, "objects DwgReader returned under the same handle");
        assertTrue(result.agrees(), "differences : " + result.differences);
        // the comparison is only meaningful if it covered the main types
        for (String type : new String[] { "LINE", "CIRCLE", "ARC", "LWPOLYLINE", "INSERT", "TEXT", "LAYER", "BLOCK_HEADER" }) {
            assertTrue(result.types.containsKey(type), "no " + type + " was compared");
        }
    }
}
