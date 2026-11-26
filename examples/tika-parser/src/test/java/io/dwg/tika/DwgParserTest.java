package io.dwg.tika;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** The parser on a sample drawing ({@code -Ddwg.samples}, default ../../samples). */
class DwgParserTest {

    private static Path sample(String name) {
        Path p = Paths.get(System.getProperty("dwg.samples", "../../samples"), name);
        Assumptions.assumeTrue(Files.exists(p), "sample not found: " + p);
        return p;
    }

    @Test
    void extractsTextAndMetadata() throws Exception {
        BodyContentHandler text = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream in = Files.newInputStream(sample("example_2000.dwg"))) {
            new DwgParser().parse(in, text, metadata, new ParseContext());
        }
        assertTrue(text.toString().contains("valoro"), text.toString());
        assertEquals("R2000", metadata.get(DwgParser.VERSION));
        assertEquals("0", metadata.get(DwgParser.READ_ISSUES));
        assertTrue(Integer.parseInt(metadata.get(DwgParser.OBJECTS)) > 0);
    }

    /** Tika finds the parser through META-INF/services and detects DWG by its content. */
    @Test
    void autoDetectParserUsesIt() throws Exception {
        BodyContentHandler text = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream in = Files.newInputStream(sample("example_2018.dwg"))) {
            new org.apache.tika.parser.AutoDetectParser().parse(in, text, metadata, new ParseContext());
        }
        assertEquals("image/vnd.dwg", metadata.get(Metadata.CONTENT_TYPE));
        assertEquals("R2018", metadata.get(DwgParser.VERSION));
        assertTrue(text.toString().contains("valoro"), text.toString());
    }

    @Test
    void notADwgFileIsATikaException() {
        assertThrows(TikaException.class, () -> new DwgParser().parse(
                new ByteArrayInputStream("not a drawing".getBytes()), new BodyContentHandler(),
                new Metadata(), new ParseContext()));
    }
}
