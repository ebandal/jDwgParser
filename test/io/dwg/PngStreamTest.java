package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.dwg.render.PngRows;
import io.dwg.render.PngWriter;

/** The row-by-row PNG writer and reader, checked against ImageIO. */
class PngStreamTest {

    @Test
    void writtenRowsReadBackByImageIo() throws IOException {
        int w = 301, h = 77;
        int[] pixels = picture(w, h);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PngWriter writer = new PngWriter(out, w, h)) {
            for (int y = 0; y < h; y++) writer.writeRow(pixels, y * w);
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(w, image.getWidth());
        assertEquals(h, image.getHeight());
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            assertEquals(pixels[y * w + x], image.getRGB(x, y) & 0xFFFFFF, x + "," + y);
        }
    }

    /** ImageIO picks its own row filters, so this reads all of them. */
    @Test
    void readsWhatImageIoWrites() throws IOException {
        int w = 257, h = 90;
        int[] pixels = picture(w, h);
        for (int type : new int[] {BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB}) {
            BufferedImage image = new BufferedImage(w, h, type);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) image.setRGB(x, y, 0xFF000000 | pixels[y * w + x]);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            int[] row = new int[w];
            try (PngRows rows = new PngRows(new ByteArrayInputStream(out.toByteArray()))) {
                assertEquals(w, rows.width());
                assertEquals(h, rows.height());
                for (int y = 0; y < h; y++) {
                    rows.readRow(row, 0);
                    for (int x = 0; x < w; x++) assertEquals(pixels[y * w + x], row[x], x + "," + y);
                }
            }
        }
    }

    @Test
    void wrongUse() throws IOException {
        assertThrows(IOException.class, () -> new PngRows(new ByteArrayInputStream("not a png at all".getBytes())));
        PngWriter writer = new PngWriter(new ByteArrayOutputStream(), 4, 2);
        writer.writeRow(new int[4], 0);
        assertThrows(IllegalStateException.class, writer::close);
    }

    /** Smooth areas, lines and noise, so that every filter has something to do. */
    private static int[] picture(int w, int h) {
        Random random = new Random(7);
        int[] p = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int v = x % 50 < 25 ? (x * 3) << 16 | (y * 2) << 8 | (x + y) : random.nextInt(0x1000000);
            p[y * w + x] = (x == y ? 0xFFFFFF : v) & 0xFFFFFF;
        }
        return p;
    }
}
