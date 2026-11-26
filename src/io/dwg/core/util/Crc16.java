package io.dwg.core.util;

/**
 * The 16-bit CRC of spec §2.14.1 (called "8-bit CRC" there, as it works on bytes): a table of 256
 * 16-bit values, each byte is XORed with the low byte of the CRC, the CRC is shifted right by 8
 * and XORed with the table entry. The table is that of the bit-reversed polynomial 0xA001
 * (x^16 + x^15 + x^2 + 1); its first entries are 0x0000, 0xC0C1, 0xC181, 0x0140, as in the
 * specification's listing.
 */
public final class Crc16 {

    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0xA001 : crc >>> 1;
            }
            TABLE[i] = crc;
        }
    }

    private Crc16() {
    }

    /** The CRC of {@code length} bytes from {@code offset}, starting from {@code seed}. */
    public static int of(int seed, byte[] data, int offset, int length) {
        int crc = seed & 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc = (crc >>> 8) ^ TABLE[(crc ^ data[i]) & 0xFF];
        }
        return crc;
    }

    /** The table entry {@code i}, for checks against the specification's listing. */
    static int entry(int i) {
        return TABLE[i];
    }
}
