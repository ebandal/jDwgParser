package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.api.ReadIssue;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.DwgObject;
import io.dwg.entities.XDataRecord;
import io.dwg.entities.concrete.DwgDictionary;
import io.dwg.entities.concrete.DwgXrecord;
import io.dwg.sections.classes.DwgClassDefinition;

/** Classes, dictionaries and XRECORDs of the same drawing in every version. */
class ClassesAndDictionariesTest {

    private static final String[] FILES = {
        "example_r14.dwg", "example_2000.dwg", "example_2004.dwg", "example_2007.dwg",
        "example_2010.dwg", "example_2013.dwg", "example_2018.dwg",
    };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void classesAreNumberedInOrderAndKnowEntitiesFromObjects() throws Exception {
        for (String file : FILES) {
            List<ReadIssue> issues = new ArrayList<>();
            DwgDocument doc = DwgReader.defaultReader().onIssue(issues::add).open(Paths.get(Samples.path(file)));
            List<DwgClassDefinition> classes = doc.customClasses();
            assertFalse(classes.isEmpty(), file);
            for (int i = 0; i < classes.size(); i++) {
                assertEquals(500 + i, classes.get(i).classNumber(), file);
            }
            assertEquals(Boolean.FALSE, entityFlag(classes, "DICTIONARYVAR"), file);
            assertEquals(Boolean.TRUE, entityFlag(classes, "ACAD_TABLE"), file);
            assertTrue(issues.stream().noneMatch(i -> i.kind() == ReadIssue.Kind.SECTION), file + " " + issues);
            assertEquals(issues, doc.issues(), file);
        }
    }

    @Test
    void namedObjectDictionaryAndXrecordsAreRead() throws Exception {
        for (String file : FILES) {
            DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
            Map<Long, DwgObject> objects = doc.objectMap();
            // the named object dictionary is owned by no one and holds ACAD_GROUP
            DwgDictionary root = objects.values().stream()
                    .filter(o -> o instanceof DwgDictionary && ((DwgDictionary) o).getEntry("ACAD_GROUP") != null)
                    .map(o -> (DwgDictionary) o).findFirst().orElse(null);
            assertNotNull(root, file);
            for (DwgHandleRef entry : root.entries().values()) {
                assertTrue(objects.containsKey(entry.rawHandle()), file + " entry " + entry);
            }
            int xrecords = 0;
            for (DwgObject o : objects.values()) {
                if (o instanceof DwgXrecord) {
                    xrecords++;
                    for (XDataRecord item : ((DwgXrecord) o).items()) {
                        assertTrue(item.groupCode() >= 0, file + " XRECORD " + Long.toHexString(o.handle()) + " not read to its end");
                    }
                }
            }
            assertTrue(xrecords > 0, file);
        }
    }

    private static Boolean entityFlag(List<DwgClassDefinition> classes, String dxfName) {
        return classes.stream().filter(c -> dxfName.equals(c.dxfRecordName())).map(DwgClassDefinition::isEntity)
                .findFirst().orElse(null);
    }
}
