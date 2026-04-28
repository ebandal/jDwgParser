package test.java.phase9;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.ByteUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Debug R2010 header decompression to find the exact bug
 */
public class DebugR2010Decompression {

    public static void main(String[] args) throws Exception {
        setupLogging();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      DEBUG: R2007 vs R2010 HEADER DECOMPRESSION           ║");
        System.out.println("║      Find exact point of failure in Lz77Decompressor     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Test both R2007 and R2010 files
        testFile("DWG/example_2007.dwg");
        System.out.println("\n" + "=".repeat(70) + "\n");
        testFile("DWG/example_2010.dwg");
    }

    static void testFile(String filename) throws Exception {
        Path file = Paths.get(filename);
        if (!Files.exists(file)) {
            System.out.println("File not found: " + filename);
            return;
        }

        System.out.println("Testing: " + filename);
        byte[] fileData = Files.readAllBytes(file);
        System.out.printf("File size: %d bytes\n\n", fileData.length);

        // Read header manually
        BitInput input = new ByteBufferBitInput(ByteBuffer.wrap(fileData));

        // Skip unencrypted header (0x00-0x7F, 128 bytes)
        byte[] unencryptedHeader = new byte[0x80];
        for (int i = 0; i < 0x80; i++) {
            unencryptedHeader[i] = (byte) input.readRawChar();
        }
        System.out.println("[DEBUG] Unencrypted header read (128 bytes)");

        // Read RS-encoded header (0x80-0x3d7, 952 bytes)
        byte[] rsEncodedHeader = new byte[0x3d8];
        for (int i = 0; i < 0x3d8; i++) {
            rsEncodedHeader[i] = (byte) input.readRawChar();
        }
        System.out.printf("[DEBUG] RS-encoded header read (952 bytes)\n");
        System.out.print("[DEBUG] First 32 bytes of RS data: ");
        for (int i = 0; i < 32; i++) {
            System.out.printf("%02X ", rsEncodedHeader[i] & 0xFF);
        }
        System.out.println();

        // Decode using Reed-Solomon
        System.out.println("\n[DEBUG] Attempting Reed-Solomon decode...");
        byte[] decodedHeader = ReedSolomonDecoder.decodeR2007Data(rsEncodedHeader);

        if (decodedHeader == null) {
            System.out.println("[ERROR] RS decoder returned null");
            return;
        }

        System.out.printf("[DEBUG] RS decode successful: %d bytes decoded\n", decodedHeader.length);
        System.out.print("[DEBUG] First 32 bytes of decoded data: ");
        for (int i = 0; i < Math.min(32, decodedHeader.length); i++) {
            System.out.printf("%02X ", decodedHeader[i] & 0xFF);
        }
        System.out.println();

        // Read comprLen from decoded header at offset 24
        long comprLen = ByteUtils.readLE32(decodedHeader, 24) & 0xFFFFFFFFL;
        System.out.printf("\n[DEBUG] comprLen (offset 24): %d (0x%X)\n", comprLen, comprLen);
        System.out.printf("[DEBUG] Available data from offset 32: %d bytes\n", decodedHeader.length - 32);

        if (comprLen > decodedHeader.length - 32) {
            System.out.printf("[ERROR] comprLen exceeds available data! %d > %d\n",
                comprLen, decodedHeader.length - 32);
            return;
        }

        // Extract compressed header
        int comprLenInt = (int)(comprLen & 0xFFFFFFFFL);
        byte[] compressed = new byte[comprLenInt];
        System.arraycopy(decodedHeader, 32, compressed, 0, comprLenInt);
        System.out.printf("\n[DEBUG] Compressed header extracted: %d bytes\n", compressed.length);
        System.out.print("[DEBUG] First 32 bytes of compressed data: ");
        for (int i = 0; i < Math.min(32, compressed.length); i++) {
            System.out.printf("%02X ", compressed[i] & 0xFF);
        }
        System.out.println();

        // Try to decompress
        System.out.println("\n[DEBUG] Attempting LZ77 decompression with expectedSize=272...");
        try {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            byte[] decompressed = lz77.decompress(compressed, 272);
            System.out.printf("[SUCCESS] Decompression succeeded! Output size: %d\n", decompressed.length);
        } catch (Exception e) {
            System.out.printf("[ERROR] Decompression failed: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupLogging() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.WARNING);
        for (var h : rootLogger.getHandlers()) {
            h.setLevel(Level.WARNING);
        }
    }
}
