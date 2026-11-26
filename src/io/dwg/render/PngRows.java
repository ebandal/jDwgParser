package io.dwg.render;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

/**
 * Reads a PNG image row by row (PNG, ISO/IEC 15948), so images of any size can be combined
 * without holding them in memory. Reads 8-bit RGB and RGBA images that are not interlaced, with
 * any of the five row filters (the alpha is dropped); that covers the tiles this library writes
 * and most PNG files.
 */
public final class PngRows implements AutoCloseable {
    private final DataInputStream in;
    private final InflaterInputStream data;
    private final int width;
    private final int height;
    private final int bytesPerPixel;
    private byte[] previous;
    private byte[] current;
    private int rowsRead;

    public PngRows(InputStream stream) throws IOException {
        in = new DataInputStream(stream);
        byte[] signature = new byte[8];
        in.readFully(signature);
        if ((signature[0] & 0xFF) != 0x89 || signature[1] != 'P' || signature[2] != 'N' || signature[3] != 'G') {
            throw new IOException("not a PNG file");
        }
        int length = in.readInt();
        byte[] type = new byte[4];
        in.readFully(type);
        if (length != 13 || !new String(type, "US-ASCII").equals("IHDR")) {
            throw new IOException("PNG file without IHDR");
        }
        width = in.readInt();
        height = in.readInt();
        int bitDepth = in.readUnsignedByte();
        int colorType = in.readUnsignedByte();
        in.readUnsignedByte();                      // compression
        in.readUnsignedByte();                      // filter method
        int interlace = in.readUnsignedByte();
        in.readInt();                               // CRC
        if (bitDepth != 8 || (colorType != 2 && colorType != 6) || interlace != 0) {
            throw new IOException("only 8-bit RGB or RGBA PNGs that are not interlaced can be read row by row");
        }
        bytesPerPixel = colorType == 2 ? 3 : 4;
        previous = new byte[width * bytesPerPixel];
        current = new byte[width * bytesPerPixel];
        data = new InflaterInputStream(new IdatStream());
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Reads the next row into {@code pixels[offset..]} as 0xRRGGBB. */
    public void readRow(int[] pixels, int offset) throws IOException {
        if (rowsRead >= height) {
            throw new IllegalStateException("all " + height + " rows are read");
        }
        int filter = data.read();
        if (filter < 0) {
            throw new EOFException("PNG data ends at row " + rowsRead);
        }
        readFully(current);
        unfilter(filter);
        for (int x = 0, i = 0; x < width; x++, i += bytesPerPixel) {
            pixels[offset + x] = (current[i] & 0xFF) << 16 | (current[i + 1] & 0xFF) << 8 | (current[i + 2] & 0xFF);
        }
        byte[] t = previous;
        previous = current;
        current = t;
        rowsRead++;
    }

    private void unfilter(int filter) throws IOException {
        int bpp = bytesPerPixel;
        byte[] c = current, p = previous;
        boolean first = rowsRead == 0;              // the row above the first is all zeros
        if (first) {
            Arrays.fill(p, (byte) 0);
        }
        switch (filter) {
            case 0:
                break;
            case 1:
                for (int i = bpp; i < c.length; i++) c[i] += c[i - bpp];
                break;
            case 2:
                for (int i = 0; i < c.length; i++) c[i] += p[i];
                break;
            case 3:
                for (int i = 0; i < c.length; i++) {
                    int left = i >= bpp ? c[i - bpp] & 0xFF : 0;
                    c[i] += (byte) ((left + (p[i] & 0xFF)) >>> 1);
                }
                break;
            case 4:
                for (int i = 0; i < c.length; i++) {
                    int a = i >= bpp ? c[i - bpp] & 0xFF : 0;
                    int b = p[i] & 0xFF;
                    int cc = i >= bpp ? p[i - bpp] & 0xFF : 0;
                    int pa = Math.abs(b - cc), pb = Math.abs(a - cc), pc = Math.abs(a + b - 2 * cc);
                    c[i] += (byte) (pa <= pb && pa <= pc ? a : pb <= pc ? b : cc);
                }
                break;
            default:
                throw new IOException("unknown PNG row filter " + filter);
        }
    }

    private void readFully(byte[] b) throws IOException {
        int n = 0;
        while (n < b.length) {
            int r = data.read(b, n, b.length - n);
            if (r < 0) {
                throw new EOFException("PNG data ends at row " + rowsRead);
            }
            n += r;
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /** The data of the IDAT chunks, one after another; the other chunks are skipped. */
    private final class IdatStream extends InputStream {
        private int left;
        private boolean ended;

        private boolean next() throws IOException {
            while (left == 0 && !ended) {
                int length = in.readInt();
                byte[] type = new byte[4];
                in.readFully(type);
                String t = new String(type, "US-ASCII");
                if (t.equals("IDAT")) {
                    left = length;
                    if (left == 0) {
                        in.readInt();
                    }
                } else if (t.equals("IEND")) {
                    ended = true;
                } else {
                    in.skipBytes(length);
                    in.readInt();
                }
            }
            return left > 0;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (left == 0 && !next()) {
                return -1;
            }
            int n = in.read(b, off, Math.min(len, left));
            if (n < 0) {
                throw new EOFException("PNG file ends inside a chunk");
            }
            left -= n;
            if (left == 0) {
                in.readInt();                       // the chunk's CRC
            }
            return n;
        }
    }
}
