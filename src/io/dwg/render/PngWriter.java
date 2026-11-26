package io.dwg.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Writes a PNG image row by row, so an image of any size can be written without holding it in
 * memory (PNG, ISO/IEC 15948): 8-bit RGB, not interlaced, each row with the Sub filter, the
 * compressed data in IDAT chunks of up to 64 KB.
 */
public final class PngWriter implements AutoCloseable {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int CHUNK = 64 * 1024;

    private final OutputStream out;
    private final int width;
    private final int height;
    private final Deflater deflater = new Deflater(6);
    private final byte[] row;
    private final byte[] buffer = new byte[CHUNK];
    private final ByteArrayOutputStream idat = new ByteArrayOutputStream(CHUNK);
    private int rowsWritten;

    public PngWriter(OutputStream out, int width, int height) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image size " + width + " x " + height);
        }
        this.out = out;
        this.width = width;
        this.height = height;
        this.row = new byte[1 + 3 * width];
        out.write(SIGNATURE);
        byte[] ihdr = new byte[13];
        int32(ihdr, 0, width);
        int32(ihdr, 4, height);
        ihdr[8] = 8;        // bit depth
        ihdr[9] = 2;        // color type: RGB
        chunk("IHDR", ihdr, ihdr.length);
    }

    /** Writes the next row: {@code width} pixels as 0xRRGGBB from {@code pixels[offset]}. */
    public void writeRow(int[] pixels, int offset) throws IOException {
        if (rowsWritten >= height) {
            throw new IllegalStateException("all " + height + " rows are written");
        }
        row[0] = 1;         // Sub: each byte minus the byte of the pixel to its left
        int pr = 0, pg = 0, pb = 0;
        for (int x = 0, i = 1; x < width; x++) {
            int p = pixels[offset + x];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            row[i++] = (byte) (r - pr);
            row[i++] = (byte) (g - pg);
            row[i++] = (byte) (b - pb);
            pr = r;
            pg = g;
            pb = b;
        }
        deflater.setInput(row);
        while (!deflater.needsInput()) {
            drain(Deflater.NO_FLUSH);
        }
        rowsWritten++;
    }

    /** Ends the image; all rows must have been written. */
    @Override
    public void close() throws IOException {
        if (rowsWritten != height) {
            throw new IllegalStateException(rowsWritten + " of " + height + " rows written");
        }
        deflater.finish();
        while (!deflater.finished()) {
            drain(Deflater.NO_FLUSH);
        }
        deflater.end();
        flushIdat();
        chunk("IEND", new byte[0], 0);
        out.flush();
    }

    private void drain(int flush) throws IOException {
        int n = deflater.deflate(buffer, 0, buffer.length, flush);
        idat.write(buffer, 0, n);
        if (idat.size() >= CHUNK) {
            flushIdat();
        }
    }

    private void flushIdat() throws IOException {
        if (idat.size() > 0) {
            chunk("IDAT", idat.toByteArray(), idat.size());
            idat.reset();
        }
    }

    private void chunk(String type, byte[] data, int length) throws IOException {
        byte[] head = new byte[8];
        int32(head, 0, length);
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(t, 0, head, 4, 4);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data, 0, length);
        out.write(head);
        out.write(data, 0, length);
        byte[] c = new byte[4];
        int32(c, 0, (int) crc.getValue());
        out.write(c);
    }

    private static void int32(byte[] b, int at, int v) {
        b[at] = (byte) (v >>> 24);
        b[at + 1] = (byte) (v >>> 16);
        b[at + 2] = (byte) (v >>> 8);
        b[at + 3] = (byte) v;
    }
}
