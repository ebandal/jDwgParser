package debug;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Debug R2004 header parsing for comparison
 */
public class DebugR2004HeaderParse {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2004\\Arc.dwg");

        if (!Files.exists(testFile)) {
            System.err.println("Test file not found: " + testFile);
            return;
        }

        byte[] fileData = Files.readAllBytes(testFile);
        BitInput input = new ByteBufferBitInput(fileData);

        System.out.println("=== R2004 Header Parsing Debug (Arc.dwg) ===");
        System.out.println("File size: " + fileData.length + " bytes\n");

        // 1. Version string (6 bytes): AC1018
        System.out.println("1. Version string (6 bytes):");
        byte[] versionBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            versionBytes[i] = (byte) input.readRawChar();
            System.out.printf("   [0x%02X] %c (0x%02X)\n", i, versionBytes[i], versionBytes[i] & 0xFF);
        }
        String version = new String(versionBytes, java.nio.charset.StandardCharsets.US_ASCII);
        System.out.println("   Version: " + version);

        // 2. Reserved (6 bytes)
        System.out.println("\n2. Reserved (6 bytes):");
        for (int i = 0; i < 6; i++) {
            int b = input.readRawChar();
            System.out.printf("   [%d] 0x%02X\n", i, b);
        }

        // 3. RC (1 byte): unknown_0
        System.out.println("\n3. unknown_0 (RC, 1 byte):");
        int unknown0 = input.readRawChar();
        System.out.printf("   0x%02X\n", unknown0);

        // 4. RL (4 bytes): preview address
        System.out.println("\n4. preview address (RL, 4 bytes, little-endian):");
        int previewAddr = input.readRawLong();
        System.out.printf("   0x%08X (%d)\n", previewAddr, previewAddr);

        // 5. RC (1 byte): Dwg version
        System.out.println("\n5. dwg_version (RC, 1 byte):");
        int dwgVersion = input.readRawChar();
        System.out.printf("   0x%02X\n", dwgVersion);

        // 6. RC (1 byte): Maintenance version
        System.out.println("\n6. maint_version (RC, 1 byte):");
        int maintVersion = input.readRawChar();
        System.out.printf("   0x%02X\n", maintVersion);

        // 7. RS (2 bytes): Codepage
        System.out.println("\n7. codepage (RS, 2 bytes, little-endian):");
        short codePage = input.readRawShort();
        System.out.printf("   0x%04X (%d)\n", codePage & 0xFFFF, codePage);

        // 8. RC (1 byte): Number of sections
        System.out.println("\n8. section_count (RC, 1 byte):");
        int sectionCount = input.readRawChar();
        System.out.printf("   0x%02X (%d)\n", sectionCount, sectionCount);

        long currentPos = input.position();
        System.out.printf("\nCurrent position: bit %d (byte 0x%X)\n", currentPos, currentPos / 8);

        // Show next 32 bytes in hex
        System.out.println("\nNext 32 bytes (hex dump):");
        for (int i = 0; i < 32; i++) {
            if (i % 16 == 0) System.out.printf("0x%02X: ", i);
            System.out.printf("%02X ", fileData[(int)(currentPos/8) + i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
    }
}
