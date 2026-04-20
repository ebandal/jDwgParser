package decode.section;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import structure.DwgVersion;

public final class DecoderHandles {
    private static final Logger log = Logger.getLogger(DecoderHandles.class.getName());

    public static Map<Long, Long> readHandles(byte[] buf, int off, DwgVersion ver) {
        Map<Long, Long> registry = new HashMap<>();
        int offset = off;

        long lastHandle = 0;
        long lastOffset = 0;
        int blockCount = 0;

        while (offset + 2 <= buf.length) {
            // Block size (RS - 2 bytes)
            short blockSize = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            blockSize = (short) (blockSize & 0xFFFF);
            offset += 2;

            if (blockSize == 2) {
                log.fine("Handles section terminated at block " + blockCount);
                break;
            }

            if (blockSize <= 2 || offset + blockSize - 2 > buf.length) {
                log.warning("Invalid block size: " + blockSize);
                break;
            }

            // Parse block data
            int blockEnd = offset + blockSize - 2;
            while (offset < blockEnd) {
                // Handle delta (MC - modular char)
                long handleDelta = readModularChar(buf, offset);
                if (handleDelta == 0) break;

                offset += getModularCharSize(buf, offset);

                // Offset delta (MC)
                long offsetDelta = readModularChar(buf, offset);
                offset += getModularCharSize(buf, offset);

                lastHandle += handleDelta;
                lastOffset += offsetDelta;
                registry.put(lastHandle, lastOffset);
            }

            // Skip CRC (RS - 2 bytes)
            if (offset + 2 <= buf.length) {
                offset += 2;
            }

            blockCount++;
        }

        log.info("Read " + registry.size() + " handle entries from " + blockCount + " blocks");
        return registry;
    }

    private static long readModularChar(byte[] buf, int off) {
        if (off >= buf.length) return 0;

        byte b1 = buf[off];
        if ((b1 & 0x80) == 0) {
            return b1 & 0x7F;
        } else if ((b1 & 0x40) == 0) {
            if (off + 2 > buf.length) return 0;
            return (((b1 & 0x3F) << 8) | (buf[off + 1] & 0xFF));
        } else {
            if (off + 4 > buf.length) return 0;
            return (((b1 & 0x3F) << 24) | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8) | (buf[off + 3] & 0xFF));
        }
    }

    private static int getModularCharSize(byte[] buf, int off) {
        if (off >= buf.length) return 1;
        byte b = buf[off];
        if ((b & 0x80) == 0) return 1;
        if ((b & 0x40) == 0) return 2;
        return 4;
    }
}
