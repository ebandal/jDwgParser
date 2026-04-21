package test;

import decode.util.ReedSolomon;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestRSDecoder {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Reed-Solomon Decoder Test ===\n");

        // Test 1: Perfect data (no errors)
        System.out.println("Test 1: Perfect block (no errors)");
        testPerfectBlock();

        // Test 2: Data with correctable errors
        System.out.println("\nTest 2: Block with 1 correctable error");
        testSingleError();

        // Test 3: Real R2007 header data
        System.out.println("\nTest 3: Real R2007 Arc.dwg header");
        testR2007RealData();
    }

    private static void testPerfectBlock() {
        // Create a simple test block with known values
        byte[] block = new byte[255];
        for (int i = 0; i < 239; i++) {
            block[i] = (byte)(i & 0xFF);
        }

        System.out.println("  Input block (first 10 bytes): " + formatBytes(block, 0, 10));
        int result = ReedSolomon.decodeBlock(block, false);
        System.out.println("  Decode result (no fix): " + (result == 0 ? "OK (no errors)" : "ERROR (code=" + result + ")"));
        System.out.println("  Block after (first 10 bytes): " + formatBytes(block, 0, 10));
    }

    private static void testSingleError() {
        byte[] block = new byte[255];
        for (int i = 0; i < 239; i++) {
            block[i] = (byte)(i & 0xFF);
        }

        // Add single bit error at position 50
        block[50] ^= 0x01;
        System.out.println("  Injected error at position 50");
        System.out.println("  Block[50] = 0x" + String.format("%02X", block[50] & 0xFF));

        int result = ReedSolomon.decodeBlock(block, true);
        System.out.println("  Decode result (with fix): " + result);
        System.out.println("  Block[50] after = 0x" + String.format("%02X", block[50] & 0xFF));
        System.out.println("  Expected: 0x" + String.format("%02X", 50 & 0xFF));
    }

    private static void testR2007RealData() throws Exception {
        // Read real R2007 Arc.dwg file
        String path = "samples/2007/Arc.dwg";
        byte[] fileData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));

        System.out.println("  File: " + path);
        System.out.println("  Size: " + fileData.length + " bytes");

        if (fileData.length < 0x457) {
            System.out.println("  ERROR: File too small");
            return;
        }

        // Extract RS-encoded header (0x80-0x457 = 984 bytes, but should be 765 for 3 blocks)
        byte[] rsData = new byte[765];
        System.arraycopy(fileData, 0x80, rsData, 0, 765);

        System.out.println("  RS-encoded data: " + formatBytes(rsData, 0, 20));

        byte[] decoded = ReedSolomon.decodeR2007Data(rsData);
        if (decoded == null) {
            System.out.println("  ERROR: Decoding failed, returned null");
            return;
        }

        System.out.println("  Decoded (first 20 bytes): " + formatBytes(decoded, 0, 20));

        // Parse header values
        ByteBuffer bb = ByteBuffer.wrap(decoded);
        bb.order(ByteOrder.BIG_ENDIAN);

        int headerSize = bb.getInt(0);
        System.out.println("  headerSize = 0x" + String.format("%08X", headerSize) +
                          " (decimal: " + headerSize + ")");

        long pageMapOffset = bb.getLong(4);
        System.out.println("  pageMapOffset = 0x" + String.format("%016X", pageMapOffset));

        long pagesMapId = bb.getLong(12);
        System.out.println("  pagesMapId = 0x" + String.format("%016X", pagesMapId));

        // Sanity checks
        if (headerSize > 0 && headerSize < 0x10000) {
            System.out.println("  ✓ headerSize looks reasonable");
        } else {
            System.out.println("  ✗ headerSize looks wrong (should be 0x100-0x400)");
        }

        if (pageMapOffset > 0 && pageMapOffset < 0x1000000) {
            System.out.println("  ✓ pageMapOffset looks reasonable");
        } else {
            System.out.println("  ✗ pageMapOffset looks wrong");
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
