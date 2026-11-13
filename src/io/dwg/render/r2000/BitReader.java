package io.dwg.render.r2000;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * MSB-first bit stream reader over a byte array.
 * Implements the compressed data types of the DWG specification (chapter 2).
 */
public class BitReader {
    private final byte[] buf;
    private long bitPos;
    private Charset charset = StandardCharsets.ISO_8859_1;

    public BitReader(byte[] buf, int byteOffset) {
        this.buf = buf;
        this.bitPos = (long) byteOffset * 8;
    }

    /** character set of TV texts (the code page of the drawing) */
    public BitReader charset(Charset charset) {
        if (charset != null) {
            this.charset = charset;
        }
        return this;
    }

    public long bitPosition() {
        return bitPos;
    }

    public int bytePosition() {
        return (int) (bitPos >> 3);
    }

    public void seekBit(long bitPos) {
        this.bitPos = bitPos;
    }

    public void seekByte(int byteOffset) {
        this.bitPos = (long) byteOffset * 8;
    }

    /** B : 1 bit */
    public boolean readBit() {
        int b = buf[(int) (bitPos >> 3)] >> (7 - (int) (bitPos & 7)) & 1;
        bitPos++;
        return b == 1;
    }

    /** n bits (n <= 32), most significant bit first */
    public int readBits(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            v = (v << 1) | (readBit() ? 1 : 0);
        }
        return v;
    }

    /** RC : raw char, unsigned */
    public int readRC() {
        return readBits(8);
    }

    /** RS : raw short, little-endian, unsigned */
    public int readRS() {
        int lo = readRC();
        int hi = readRC();
        return (hi << 8) | lo;
    }

    /** RL : raw long, little-endian */
    public int readRL() {
        int lo = readRS();
        int hi = readRS();
        return (hi << 16) | lo;
    }

    /** RD : raw double, little-endian */
    public double readRD() {
        long lo = readRL() & 0xFFFFFFFFL;
        long hi = readRL() & 0xFFFFFFFFL;
        return Double.longBitsToDouble((hi << 32) | lo);
    }

    /** BB : 2 bits */
    public int readBB() {
        return readBits(2);
    }

    /** BS : bitshort */
    public int readBS() {
        switch (readBB()) {
        case 0:  return readRS();
        case 1:  return readRC();
        case 2:  return 0;
        default: return 256;
        }
    }

    /** BL : bitlong */
    public int readBL() {
        switch (readBB()) {
        case 0:  return readRL();
        case 1:  return readRC();
        default: return 0;
        }
    }

    /** BD : bitdouble */
    public double readBD() {
        switch (readBB()) {
        case 0:  return readRD();
        case 1:  return 1.0;
        default: return 0.0;
        }
    }

    /** 3BD : three bitdoubles */
    public double[] read3BD() {
        return new double[] { readBD(), readBD(), readBD() };
    }

    /** 2RD : two raw doubles */
    public double[] read2RD() {
        return new double[] { readRD(), readRD() };
    }

    /** DD : bitdouble with default (R2000+) */
    public double readDD(double def) {
        int code = readBB();
        if (code == 0) {
            return def;
        }
        if (code == 3) {
            return readRD();
        }
        long bits = Double.doubleToRawLongBits(def);
        if (code == 2) {
            // 2 bytes replace bytes 5 and 6 of the default, then 4 bytes replace bytes 1-4
            long b4 = readRC();
            long b5 = readRC();
            bits = (bits & 0xFFFF0000FFFFFFFFL) | (b4 << 32) | (b5 << 40);
        }
        long lo = readRL() & 0xFFFFFFFFL;
        bits = (bits & 0xFFFFFFFF00000000L) | lo;
        return Double.longBitsToDouble(bits);
    }

    /** BT : bit thickness (R2000+) */
    public double readBT() {
        return readBit() ? 0.0 : readBD();
    }

    /** BE : bit extrusion (R2000+) */
    public double[] readBE() {
        return readBit() ? new double[] { 0.0, 0.0, 1.0 } : read3BD();
    }

    /** MC (unsigned) : modular char, 7 bits per byte, least significant byte first */
    public long readUMC() {
        long v = 0;
        int shift = 0;
        while (true) {
            int b = readRC();
            v |= (long) (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                return v;
            }
        }
    }

    /** MC (signed) : as UMC, but bit 0x40 of the last byte is the sign */
    public long readMC() {
        long v = 0;
        int shift = 0;
        while (true) {
            int b = readRC();
            if ((b & 0x80) == 0) {
                v |= (long) (b & 0x3F) << shift;
                return (b & 0x40) != 0 ? -v : v;
            }
            v |= (long) (b & 0x7F) << shift;
            shift += 7;
        }
    }

    /** MS : modular short, 15 bits per little-endian word */
    public int readMS() {
        int v = 0;
        int shift = 0;
        while (true) {
            int w = readRS();
            v |= (w & 0x7FFF) << shift;
            shift += 15;
            if ((w & 0x8000) == 0) {
                return v;
            }
        }
    }

    /** H : handle reference. Returns { code, value } */
    public long[] readHandle() {
        int code = readBits(4);
        int counter = readBits(4);
        long v = 0;
        for (int i = 0; i < counter; i++) {
            v = (v << 8) | readRC();
        }
        return new long[] { code, v };
    }

    /**
     * H : handle reference resolved to an absolute handle.
     * Codes 6, 8, 0xA and 0xC are relative to the handle of the object that contains the reference.
     */
    public long readHandleRef(long containingHandle) {
        long[] h = readHandle();
        switch ((int) h[0]) {
        case 0x6:   return containingHandle + 1;
        case 0x8:   return containingHandle - 1;
        case 0xA:   return containingHandle + h[1];
        case 0xC:   return containingHandle - h[1];
        default:    return h[1];
        }
    }

    /** TV : text, bitshort length followed by single-byte characters (R2004 and earlier) */
    public String readTV() {
        int len = readBS();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            b[i] = (byte) readRC();
        }
        // strip trailing NUL
        while (len > 0 && b[len - 1] == 0) {
            len--;
        }
        return new String(b, 0, len, charset);
    }
}
