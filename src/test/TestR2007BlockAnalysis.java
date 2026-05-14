package test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze R2007 RS-encoded blocks to understand their structure
 */
public class TestR2007BlockAnalysis {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2007 Block Structure Analysis ===\n");

        byte[] fileData = Files.readAllBytes(Paths.get("samples/2007/Arc.dwg"));
        System.out.println("File: samples/2007/Arc.dwg");
        System.out.println("Size: " + fileData.length + " bytes\n");

        // Extract RS-encoded header
        byte[] rsData = new byte[765];
        System.arraycopy(fileData, 0x80, rsData, 0, 765);

        // Analyze each 255-byte block
        for (int blk = 0; blk < 3; blk++) {
            System.out.println("=== Block " + blk + " ===");
            byte[] block = new byte[255];
            System.arraycopy(rsData, blk * 255, block, 0, 255);

            // Show structure
            System.out.println("Bytes 0-15 (hex):   " + formatBytes(block, 0, 16));
            System.out.println("Bytes 20-35 (hex):  " + formatBytes(block, 20, 16));
            System.out.println("Bytes 239-254 (parity): " + formatBytes(block, 239, 16));

            // Try interpreting first 239 bytes as data
            System.out.println("\nAssuming first 239 bytes are data:");

            // Look for patterns
            int zeroCount = 0;
            int nonZeroCount = 0;
            int printableCount = 0;
            for (int i = 0; i < 239; i++) {
                int b = block[i] & 0xFF;
                if (b == 0) zeroCount++;
                else nonZeroCount++;
                if (b >= 32 && b < 127) printableCount++;
            }

            System.out.println("  Zero bytes: " + zeroCount);
            System.out.println("  Non-zero bytes: " + nonZeroCount);
            System.out.println("  Printable ASCII: " + printableCount);

            // Try to read as little-endian integers
            ByteBuffer bb = ByteBuffer.wrap(block);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            System.out.println("\nAs LE 64-bit ints:");
            for (int i = 0; i < Math.min(8, 239 / 8); i++) {
                long val = bb.getLong(i * 8);
                System.out.println("  [" + i + "] = 0x" + String.format("%016X", val) +
                                 " (" + val + ")");
            }

            System.out.println();
        }

        // Now try manually interpreting as 717 bytes
        System.out.println("=== Combined Data (First 239 bytes each) ===");
        byte[] combined = new byte[717];
        for (int blk = 0; blk < 3; blk++) {
            System.arraycopy(rsData, blk * 255, combined, blk * 239, 239);
        }

        ByteBuffer bb = ByteBuffer.wrap(combined);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        System.out.println("First fields from combined:");
        System.out.println("  headerSize (LE64): 0x" + String.format("%016X", bb.getLong(0x00)));
        System.out.println("  fileSize (LE64):   0x" + String.format("%016X", bb.getLong(0x08)));
        System.out.println("  @0x38 pageMapOff:  0x" + String.format("%016X", bb.getLong(0x38)));
        System.out.println("  @0x40 pagesMapId:  0x" + String.format("%016X", bb.getLong(0x40)));
        System.out.println("  @0xA0 numSections: 0x" + String.format("%016X", bb.getLong(0xA0)));

        // Check sanity
        long headerSize = bb.getLong(0);
        long numSections = bb.getLong(0xA0);

        System.out.println("\nSanity checks:");
        System.out.println("  headerSize = " + headerSize + " (should be 0x100-0x1000)");
        System.out.println("  numSections = " + numSections + " (should be 1-20)");

        if (headerSize > 0 && headerSize < 0x10000) {
            System.out.println("  ✓ headerSize looks valid");
        } else {
            System.out.println("  ✗ headerSize is garbage");
        }

        if (numSections > 0 && numSections < 100) {
            System.out.println("  ✓ numSections looks valid");
        } else {
            System.out.println("  ✗ numSections is garbage");
        }
    }

    private static String formatBytes(byte[] data, int offset, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + count && i < data.length; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        return sb.toString();
    }
}
