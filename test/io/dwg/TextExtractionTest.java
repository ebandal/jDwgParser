package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.api.PlainText;

/** Plain text of TEXT and MTEXT, and the text of a whole drawing. */
class TextExtractionTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void controlCodesOfSingleLineText() {
        assertEquals("Ø35 ±0.1 90° 100%", PlainText.ofText("%%c35 %%p0.1 90%%d 100%%%"));
        assertEquals("underlined", PlainText.ofText("%%uunderlined%%u"));
        assertEquals("가", PlainText.ofText("\\U+AC00"));
    }

    @Test
    void formattingCodesOfMText() {
        assertEquals("Hello\nWorld", PlainText.ofMText("{\\fArial|b0|i0|c0|p34;Hello}\\PWorld"));
        assertEquals("1/2 inch", PlainText.ofMText("\\S1^2; inch"));
        assertEquals("a b {c}", PlainText.ofMText("\\H2.5;a\\~b \\{c\\}"));
    }

    @Test
    void textsOfADrawingAreTheSameInEveryVersion() throws Exception {
        List<String> r2000 = texts("example_2000.dwg");
        assertTrue(r2000.contains("valoro"), r2000.toString());
        assertTrue(r2000.contains("test"), "table cell: " + r2000);
        for (String file : new String[] {"example_2004.dwg", "example_2007.dwg", "example_2010.dwg",
                                         "example_2013.dwg", "example_2018.dwg"}) {
            assertEquals(r2000, texts(file), file);
        }
    }

    private static List<String> texts(String file) throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
        return doc.texts();
    }
}
