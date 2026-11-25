package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.DwgDimStyle;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.entities.concrete.DwgMLineStyle;
import io.dwg.entities.concrete.DwgStyle;

/**
 * The table records (and MLINESTYLE) read the same from every version of a drawing, and the
 * points settled with the samples hold.
 */
class TableRecordTest {

    private static final String[] VERSIONS = { "2004", "2007", "2010", "2013", "2018" };
    private static final String[] DRAWINGS = { "Leader.dwg", "Text.dwg", "Multiline.dwg" };

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void recordsAgreeWithTheR2000Copies() throws Exception {
        List<String> differences = new ArrayList<>();
        for (String drawing : DRAWINGS) {
            Map<String, String> expected = describe(open("2000/" + drawing));
            assertFalse(expected.isEmpty(), drawing);
            for (String version : VERSIONS) {
                Map<String, String> actual = describe(open(version + "/" + drawing));
                for (Map.Entry<String, String> e : expected.entrySet()) {
                    if (!e.getValue().equals(actual.get(e.getKey()))) {
                        differences.add(version + "/" + drawing + " " + e.getKey() + ": " + actual.get(e.getKey()) + " != " + e.getValue());
                    }
                }
            }
        }
        assertEquals(List.of(), differences.subList(0, Math.min(10, differences.size())));
    }

    @Test
    void xrefDependentRecordsAreMarked() throws Exception {
        int dependent = 0;
        for (DwgObject o : open("2013/gh44-error.dwg").objectMap().values()) {
            if (o instanceof AbstractDwgTableRecord) {
                AbstractDwgTableRecord record = (AbstractDwgTableRecord) o;
                boolean fromXref = record.name().contains("|");
                assertEquals(fromXref, record.isXrefDependent(), record.name());
                if (fromXref) {
                    dependent++;
                    assertFalse(record.xrefBlockHandle().isNull(), record.name());
                }
            }
        }
        assertEquals(78, dependent);
    }

    @Test
    void linetypeTextIsRead() throws Exception {
        DwgLtype ltype = open("2013/gh44-error.dwg").objectMap().values().stream()
                .filter(o -> o instanceof DwgLtype && ((DwgLtype) o).name().equals("SPRLINETYPE"))
                .map(o -> (DwgLtype) o).findFirst().orElse(null);
        assertNotNull(ltype);
        List<String> texts = ltype.dashes().stream()
                .filter(d -> (d.shapeFlag() & DwgLtype.SHAPE_IS_TEXT) != 0)
                .map(DwgLtype.Dash::text).collect(Collectors.toList());
        assertEquals(List.of("SPR", "SPR"), texts);
        assertEquals(395.0, ltype.patternLength());
    }

    private static DwgDocument open(String relative) throws Exception {
        return DwgReader.defaultReader().open(Paths.get(Samples.path(relative)));
    }

    /** The values that every version holds, by type and name. */
    private static Map<String, String> describe(DwgDocument doc) {
        Map<String, String> m = new TreeMap<>();
        for (DwgObject o : doc.objectMap().values()) {
            if (o instanceof DwgLtype) {
                DwgLtype l = (DwgLtype) o;
                StringBuilder sb = new StringBuilder(l.description() + " " + l.patternLength());
                for (DwgLtype.Dash d : l.dashes()) sb.append(' ').append(d.length()).append('/').append(d.shapeFlag());
                m.put("LTYPE " + l.name(), sb.toString());
            } else if (o instanceof DwgStyle) {
                DwgStyle s = (DwgStyle) o;
                m.put("STYLE " + s.name(), s.fontFilename() + " " + s.bigFontFilename() + " " + s.width() + " "
                        + s.oblique() + " " + s.fixedHeight() + " " + s.generation() + " " + s.flags());
            } else if (o instanceof DwgDimStyle) {
                DwgDimStyle d = (DwgDimStyle) o;
                d.variables().forEach((k, v) -> {
                    if (!(v instanceof DwgHandleRef)) m.put("DIMSTYLE " + d.name() + " " + k, text(v));
                });
            } else if (o instanceof DwgLayer) {
                DwgLayer l = (DwgLayer) o;
                m.put("LAYER " + l.name(), l.color().getColorIndex() + " " + l.isOn() + " " + l.isFrozen() + " " + l.isLocked());
            } else if (o instanceof DwgMLineStyle) {
                DwgMLineStyle s = (DwgMLineStyle) o;
                StringBuilder sb = new StringBuilder(s.description() + " " + s.flags() + " " + s.startAngle() + " " + s.endAngle());
                for (DwgMLineStyle.Line line : s.lines()) sb.append(' ').append(line.offset()).append('/').append(line.color().getColorIndex());
                m.put("MLINESTYLE " + s.name(), sb.toString());
            }
        }
        return m;
    }

    private static String text(Object value) {
        return value instanceof CmColor ? "color " + ((CmColor) value).getColorIndex() : String.valueOf(value);
    }
}
