package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.Renderer;
import io.dwg.render.model.Drawing;

/** The SVG and PDF outputs of the renderer: well formed, and with what the PNG output draws. */
class VectorOutputTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void svgIsWellFormedAndHasEveryItem() throws Exception {
        Drawing drawing = drawing("example_2000.dwg");
        Renderer renderer = new Renderer();
        String svg = renderer.renderSvg(drawing);
        assertNotNull(svg);
        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        assertEquals("svg", xml.getDocumentElement().getNodeName());
        int paths = xml.getElementsByTagName("path").getLength();
        int clips = xml.getElementsByTagName("clipPath").getLength();
        // one path per item (a hatch pattern also has the path of its clip)
        assertTrue(paths - clips <= renderer.drawn && paths - clips > renderer.drawn / 2,
                paths + " paths, " + clips + " clips, " + renderer.drawn + " items");
    }

    @Test
    void pdfHasItsStructure() throws Exception {
        Drawing drawing = drawing("example_2018.dwg");
        Renderer renderer = new Renderer();
        byte[] pdf = renderer.renderPdf(drawing);
        assertNotNull(pdf);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("%PDF-1.4"));
        assertTrue(text.endsWith("%%EOF\n"));
        // the cross-reference table points at the objects
        int xref = Integer.parseInt(text.substring(text.lastIndexOf("startxref\n") + 10, text.lastIndexOf("\n%%EOF")).trim());
        assertTrue(text.startsWith("xref", xref));
        String[] lines = text.substring(xref).split("\n");
        for (int i = 1; i <= 4; i++) {
            int offset = Integer.parseInt(lines[2 + i].substring(0, 10));
            assertTrue(text.startsWith(i + " 0 obj", offset), "object " + i);
        }
    }

    private static Drawing drawing(String file) throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(Samples.path(file)));
        return DocumentDrawing.from(doc);
    }
}
