package decode.util;

/**
 * CRC-32 and CRC-8 utilities for DWG files.
 *
 * CRC-32 (R2004+ header): reflected, polynomial 0xEDB88320
 * CRC-8  (R13-R15 object map): DWG spec §23, polynomial 0x8F
 */
public final class Crc {

    public static final int[] CRC32_TABLE = new int[256];
    public static final int[] CRC8_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xEDB88320;
                } else {
                    crc = crc >>> 1;
                }
            }
            CRC32_TABLE[i] = crc;
        }

        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                int flag = (crc & 0x80) != 0 ? 1 : 0;
                crc = (crc << 1) & 0xFF;
                if (flag != 0) {
                    crc ^= 0x8F;
                }
            }
            CRC8_TABLE[i] = crc;
        }
    }

    private Crc() {}

    public static int crc32(byte[] data, int seed) {
        int crc = seed ^ 0xFFFFFFFF;
        for (byte b : data) {
            crc = CRC32_TABLE[(crc ^ b) & 0xFF] ^ (crc >>> 8);
        }
        return crc ^ 0xFFFFFFFF;
    }

    public static int crc32(byte[] data) {
        return crc32(data, 0);
    }

    public static int crc8(byte[] data, int seed) {
        int crc = seed;
        for (byte b : data) {
            crc = CRC8_TABLE[(crc ^ (b & 0xFF)) & 0xFF];
        }
        return crc & 0xFF;
    }
}
