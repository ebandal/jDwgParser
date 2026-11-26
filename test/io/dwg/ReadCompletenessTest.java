package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.api.ReadIssue;
import io.dwg.entities.DwgObject;

/**
 * For the object types whose readers have been checked against the specification and the
 * samples, every object of every sample is read to the end of its data and of its handles: the
 * reader reports no {@link ReadIssue} for them. A type is added here once that holds for it.
 */
class ReadCompletenessTest {

    private static final Set<String> CHECKED = Set.of(
            "LINE", "CIRCLE", "ARC", "POINT", "ELLIPSE", "SPLINE", "LWPLINE", "SOLID", "TRACE", "FACE3D",
            "TEXT", "MTEXT", "ATTRIB", "ATTDEF", "INSERT", "MINSERT", "BLOCK", "ENDBLK", "SEQEND",
            "RAY", "XLINE", "LEADER",
            "DIMENSION_LINEAR", "DIMENSION_ALIGNED", "DIMENSION_ORDINATE", "DIMENSION_ANG_2LN",
            "DIMENSION_ANG_3PT", "DIMENSION_RADIUS", "DIMENSION_DIAMETER",
            "POLYLINE_2D", "POLYLINE_3D", "POLYLINE_PFACE", "POLYLINE_MESH",
            "VERTEX_2D", "VERTEX_3D", "VERTEX_PFACE", "VERTEX_PFACE_FACE",
            "LAYER", "LTYPE", "STYLE", "DIMSTYLE", "APPID", "MLINESTYLE",
            "DICTIONARY", "XRECORD",
            "VIEW", "UCS", "VPORT", "LAYOUT", "VIEWPORT", "GROUP", "SCALE", "ACAD_DICTIONARYVAR",
            "VERTEX_MESH",
            "SHAPE", "TOLERANCE", "MLINE", "IMAGE", "WIPEOUT", "HATCH", "OLE2FRAME", "ACAD_FIELD",
            "ACAD_TABLESTYLE", "ACAD_CELLSTYLE", "MULTILEADER", "ACAD_TABLE", "BLOCK_HEADER",
            "REGION", "SOLID3D", "BODY", "ACAD_PROXY_ENTITY",
            "VISUALSTYLE", "ACAD_MATERIAL", "ACAD_PERSSUBENTMANAGER", "SURFACE", "MESH", "UNDERLAY");

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void checkedTypesAreReadToTheirEnd() throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(Paths.get(Samples.path("")))) {
            files = s.filter(p -> p.toString().toLowerCase().endsWith(".dwg")).sorted().collect(Collectors.toList());
        }
        Map<String, Integer> objects = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (Path file : files) {
            DwgDocument doc;
            try {
                doc = DwgReader.defaultReader().open(file);
            } catch (Exception e) {
                continue;                          // files that do not open are another matter
            }
            for (DwgObject o : doc.objectMap().values()) {
                objects.merge(o.objectType().name(), 1, Integer::sum);
            }
            for (ReadIssue issue : doc.issues()) {
                if (issue.objectType() != null && CHECKED.contains(issue.objectType())) {
                    problems.add(file.getFileName() + " " + issue);
                }
            }
        }
        assertEquals(List.of(), problems.subList(0, Math.min(20, problems.size())), problems.size() + " issues");
        for (String type : new String[] {"LINE", "MTEXT", "INSERT", "DIMENSION_LINEAR", "POLYLINE_2D", "XRECORD"}) {
            assertTrue(objects.getOrDefault(type, 0) > 0, "no " + type + " in the samples");
        }
    }
}
