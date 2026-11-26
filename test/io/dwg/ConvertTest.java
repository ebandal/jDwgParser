package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.dwg.api.DwgReader;
import io.dwg.cli.Convert;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.Renderer;

/** The conversion command, and the streaming read it uses. */
class ConvertTest {

    @BeforeAll
    static void samplesArePresent() {
        Samples.assumePresent();
    }

    @Test
    void convertsToPng(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("example.png");
        int status = Convert.run(new String[] {Samples.path("example_2018.dwg"), out.toString(), "--size", "800"});
        assertEquals(0, status);
        BufferedImage image = ImageIO.read(out.toFile());
        assertEquals(800, Math.max(image.getWidth(), image.getHeight()));
    }

    /** Split into tiles and merged again, the PNG is the one drawn whole. */
    @Test
    void splitAndMergeGiveTheWholePicture(@TempDir Path dir) throws Exception {
        String dwg = Samples.path("2018/Dynblocks.dwg");
        Path whole = dir.resolve("whole.png");
        assertEquals(0, Convert.run(new String[] {dwg, whole.toString(), "--dpi", "150"}));
        assertEquals(0, Convert.run(new String[] {dwg, dir.resolve("part.png").toString(), "--dpi", "150", "--split", "--tile", "300"}));
        assertTrue(Files.exists(dir.resolve("part[2x3].png")));
        Path merged = dir.resolve("merged.png");
        assertEquals(0, Convert.run(new String[] {"merge", dir.resolve("part.tiles").toString(), merged.toString()}));
        BufferedImage a = ImageIO.read(whole.toFile()), b = ImageIO.read(merged.toFile());
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getHeight(), b.getHeight());
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
            assertEquals(a.getRGB(x, y), b.getRGB(x, y), x + "," + y);
        }
        // the paper of this drawing is 216 x 279 mm: at 150 dpi its longer side is 1650 px
        assertEquals(1650, Math.max(a.getWidth(), a.getHeight()));
    }

    /** {@code --scale 1/4} gives a quarter of the size chosen; the paper at 300 dpi is 3300 px. */
    @Test
    void scaleShrinksTheChosenSize(@TempDir Path dir) throws Exception {
        String dwg = Samples.path("2018/Dynblocks.dwg");
        int[] expected = {3300, 1650, 825, 413, 206};
        String[] scales = {"1", "1/2", "1/4", "1/8", "1/16"};
        for (int i = 0; i < scales.length; i++) {
            Path out = dir.resolve("s" + i + ".png");
            assertEquals(0, Convert.run(new String[] {dwg, out.toString(), "--scale", scales[i]}));
            BufferedImage image = ImageIO.read(out.toFile());
            assertEquals(expected[i], Math.max(image.getWidth(), image.getHeight()), scales[i]);
        }
        Path half = dir.resolve("half.png");
        assertEquals(0, Convert.run(new String[] {dwg, half.toString(), "--size", "1000", "--scale", "0.5"}));
        assertEquals(500, Math.max(ImageIO.read(half.toFile()).getWidth(), ImageIO.read(half.toFile()).getHeight()));
    }

    /** --json: one JSON document on the standard output, with the result of each file. */
    @Test
    void json(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.dwg");
        Files.write(bad, "not a drawing".getBytes());
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        java.io.PrintStream old = System.out;
        int status;
        System.setOut(new java.io.PrintStream(captured, true, "UTF-8"));
        try {
            status = Convert.run(new String[] {Samples.path("2018/Dynblocks.dwg"), dir.resolve("a.png").toString(), "--json", "--scale", "1/8"});
            assertEquals(0, status);
            assertEquals(1, Convert.run(new String[] {bad.toString(), dir.resolve("b.png").toString(), "--json"}));
            assertEquals(2, Convert.run(new String[] {"a.dwg", "--size", "--json"}));
        } finally {
            System.setOut(old);
        }
        String out = captured.toString("UTF-8");
        String[] docs = out.split("(?m)^\\}\\R");
        assertEquals(3, docs.length, out);
        assertTrue(docs[0].startsWith("{") && docs[0].contains("\"status\": 0") && docs[0].contains("\"ok\": true")
                && docs[0].contains("\"width\": 413") && docs[0].contains("\"decoded\": "), docs[0]);
        assertTrue(docs[1].contains("\"status\": 1") && docs[1].contains("\"ok\": false") && docs[1].contains("\"bugReport\": \"jDwgParser "), docs[1]);
        assertTrue(docs[2].contains("\"status\": 2") && docs[2].contains("\"error\": \"a number was expected"), docs[2]);
    }

    @Test
    void version() {
        assertTrue(io.dwg.api.Version.number().matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"), io.dwg.api.Version.number());
        assertTrue(io.dwg.api.Version.describe().startsWith("jDwgParser " + io.dwg.api.Version.number() + " (built "));
        assertEquals(0, Convert.run(new String[] {"--version"}));
    }

    @Test
    void wrongArgumentsAndUnreadableFiles(@TempDir Path dir) throws Exception {
        assertEquals(2, Convert.run(new String[] {}));
        assertEquals(2, Convert.run(new String[] {"a.dwg", "--size", "x"}));
        assertEquals(2, Convert.run(new String[] {"a.dwg", "--size", "100", "--dpi", "100"}));
        assertEquals(2, Convert.run(new String[] {"merge"}));
        assertEquals(2, Convert.run(new String[] {"a.dwg", "--scale", "0"}));
        assertEquals(2, Convert.run(new String[] {"a.dwg", "--scale", "1/x"}));
        assertEquals(1, Convert.run(new String[] {"merge", dir.resolve("none.tiles").toString()}));
        Path notDwg = dir.resolve("not.dwg");
        Files.write(notDwg, "not a drawing".getBytes());
        assertEquals(1, Convert.run(new String[] {notDwg.toString(), dir.resolve("x.png").toString()}));
    }

    /** Streaming the objects into the drawing gives the same picture as reading the document. */
    @Test
    void streamingDrawsTheSamePicture() throws Exception {
        for (String file : new String[] {"example_2000.dwg", "example_2007.dwg", "2018/Dynblocks.dwg"}) {
            Path path = Paths.get(Samples.path(file));
            BufferedImage fromDocument = new Renderer().render(DocumentDrawing.from(DwgReader.defaultReader().open(path)));
            DocumentDrawing.Builder builder = new DocumentDrawing.Builder().keepObjects(false);
            DwgReader.defaultReader().stream(path, builder);
            BufferedImage streamed = new Renderer().render(builder.build());
            assertEquals(fromDocument.getWidth(), streamed.getWidth(), file);
            assertEquals(fromDocument.getHeight(), streamed.getHeight(), file);
            int differ = 0;
            for (int y = 0; y < streamed.getHeight(); y++) {
                for (int x = 0; x < streamed.getWidth(); x++) {
                    if (streamed.getRGB(x, y) != fromDocument.getRGB(x, y)) differ++;
                }
            }
            assertEquals(0, differ, file);
        }
    }
}
