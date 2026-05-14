package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.ByteUtils;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug all decompressed header fields to identify correct field offsets
 */
public class DebugAllHeaderFields {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Decompressed R2007 Header - All Fields");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("RS decode failed");
            return;
        }

        // Decompress
        int comprLen = (int)(ByteUtils.readLE32(decoded, 24) & 0xFFFFFFFFL);
        byte[] compressed = new byte[comprLen];
        System.arraycopy(decoded, 32, compressed, 0, comprLen);

        Lz77Decompressor lz77 = new Lz77Decompressor();
        byte[] decompressed = lz77.decompress(compressed, 272);

        System.out.println("LE64 values at each 8-byte offset:\n");

        for (int offset = 0; offset + 8 <= decompressed.length; offset += 8) {
            long value = ByteUtils.readLE64(decompressed, offset);
            System.out.printf("+%03d (0x%02X): 0x%016X", offset, offset, value);

            // Annotate known fields
            if (offset == 56) System.out.print("  <- pages_map_offset (expected 0x0)");
            else if (offset == 80) System.out.print("  <- pages_map_size_comp (expected 0x72)");
            else if (offset == 88) System.out.print("  <- pages_map_size_uncomp (expected 0x140)");
            else if (offset == 128) System.out.print("  <- sections_map_size_comp? (garbage)");
            else if (offset == 144) System.out.print("  <- sections_map_id? (expected small)");
            else if (offset == 152) System.out.print("  <- sections_map_size_uncomp? (garbage)");

            // Look for pattern: sections_map should have id=1, reasonable sizes
            if (value == 1 || value == 2 || value == 4) {
                System.out.print("  [POSSIBLE ID]");
            }
            if (value > 0 && value < 0x100000 && value != 0x140) {
                System.out.print("  [REASONABLE SIZE?]");
            }

            System.out.println();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Note: sections_map is typically at page ID 1, with comp/uncomp sizes");
        System.out.println("This should help identify correct field offsets");
    }
}
