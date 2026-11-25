package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.entities.XDataRecord;

/**
 * The EED of the R13–R2018 samples is read in full: every application's data begins with its
 * name, and no data is left unread.
 */
class ExtendedDataTest {

    private static final String[] FOLDERS = { "2000", "2004", "2007", "2010", "2013", "2018" };
    private static final String[] FILES = { "example_r13.dwg", "example_r14.dwg" };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void samplesExtendedDataIsReadInFull() throws Exception {
        List<File> files = new ArrayList<>();
        for (String folder : FOLDERS) {
            File[] list = new File(Samples.path(folder)).listFiles((d, n) -> n.toLowerCase().endsWith(".dwg"));
            if (list != null) files.addAll(List.of(list));
        }
        for (String name : FILES) {
            files.add(new File(Samples.path(name)));
        }

        int applications = 0;
        List<String> problems = new ArrayList<>();
        for (File file : files) {
            DwgDocument doc = DwgReader.defaultReader().open(file.toPath());
            for (Map.Entry<Long, DwgObject> e : doc.objectMap().entrySet()) {
                List<XDataRecord> records = e.getValue().xData();
                if (records.isEmpty()) continue;
                if (records.get(0).groupCode() != 1001) {
                    problems.add(file.getName() + " " + Long.toHexString(e.getKey()) + ": no application name first");
                }
                for (XDataRecord record : records) {
                    if (record.groupCode() == 1001) {
                        applications++;
                        if (!(record.value() instanceof String)) {
                            problems.add(file.getName() + " " + Long.toHexString(e.getKey()) + ": application not found " + record.value());
                        }
                    } else if (record.groupCode() < 1000) {
                        problems.add(file.getName() + " " + Long.toHexString(e.getKey()) + ": data left unread");
                    }
                }
            }
        }
        assertTrue(applications > 1000, "too few applications' data found: " + applications);
        assertEquals(List.of(), problems.subList(0, Math.min(20, problems.size())));
    }
}
