package debug;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnoseR2000ObjectsFormat {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        // Extract Objects section (0x60 to 0x6B8A)
        int objStart = 0x60;
        int objEnd = 0x6B8B;
        byte[] objectsData = new byte[objEnd - objStart];
        System.arraycopy(data, objStart, objectsData, 0, objectsData.length);

        System.out.println("=== R2000 Objects Section Format Diagnosis ===\n");
        System.out.printf("Objects section: 0x%X bytes\n\n", objectsData.length);

        // Method 1: Try to interpret as Objects stream
        System.out.println("【 Method 1: BitStreamReader Interpretation 】");
        ByteBufferBitInput buf = new ByteBufferBitInput(ByteBuffer.wrap(objectsData));
        BitStreamReader reader = new BitStreamReader(buf, DwgVersion.R2000);

        System.out.println("\nFirst 20 values read as MS/BS pairs:");
        for (int i = 0; i < 20 && reader.position() / 8 < objectsData.length - 10; i++) {
            try {
                long startBit = reader.position();
                int ms = reader.readModularShort();
                int bs = reader.readBitShort();
                long endBit = reader.position();
                long bytesUsed = (endBit - startBit) / 8;

                System.out.printf("  [%2d] MS=%6d, BS=%3d (bits: %d..%d, bytes: %d)\n",
                    i, ms, bs, startBit, endBit, bytesUsed);
            } catch (Exception e) {
                System.out.printf("  [%2d] ERROR: %s\n", i, e.getMessage());
                break;
            }
        }

        // Method 2: Look for structure patterns
        System.out.println("\n【 Method 2: Pattern Analysis 】");
        System.out.println("Looking for repeating patterns or known structures...");

        // Check if it's all zeros (placeholder)
        int zeroCount = 0;
        for (byte b : objectsData) {
            if (b == 0) zeroCount++;
        }
        System.out.printf("Zero bytes: %d / %d (%.1f%%)\n", zeroCount, objectsData.length,
            100.0 * zeroCount / objectsData.length);

        // Check byte distribution
        int[] freq = new int[256];
        for (byte b : objectsData) {
            freq[b & 0xFF]++;
        }
        System.out.println("Most common bytes:");
        for (int i = 0; i < 256; i++) {
            if (freq[i] > objectsData.length / 20) {  // Top 5%
                System.out.printf("  0x%02X: %d (%.1f%%)\n", i, freq[i], 100.0 * freq[i] / objectsData.length);
            }
        }

        // Method 3: Check for known DWG signatures
        System.out.println("\n【 Method 3: Known Signatures 】");
        checkSignatures(objectsData);
    }

    static void checkSignatures(byte[] data) {
        // Check if might be XOR encrypted (like R2004)
        System.out.println("Checking if data might be XOR encrypted...");
        // R2004 typically XORs with 0x4A5C49
        byte[] xorKey = {0x4A, 0x5C, 0x49};
        System.out.println("Sample data XORed with R2004 key:");
        System.out.print("  ");
        for (int i = 0; i < Math.min(16, data.length); i++) {
            int xored = data[i] ^ xorKey[i % 3];
            System.out.printf("%02X ", xored & 0xFF);
        }
        System.out.println();

        // Check if it starts with known opcodes
        System.out.println("\nChecking first bytes interpretation:");
        System.out.printf("  Raw: %02X %02X %02X %02X\n",
            data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF, data[3] & 0xFF);

        // Try interpreting as little-endian integers
        int le32 = (data[0] & 0xFF)
                | ((data[1] & 0xFF) << 8)
                | ((data[2] & 0xFF) << 16)
                | ((data[3] & 0xFF) << 24);
        System.out.printf("  LE32: 0x%X (%d)\n", le32, le32);

        // Try as big-endian
        int be32 = ((data[0] & 0xFF) << 24)
                | ((data[1] & 0xFF) << 16)
                | ((data[2] & 0xFF) << 8)
                | (data[3] & 0xFF);
        System.out.printf("  BE32: 0x%X (%d)\n", be32, be32);
    }
}
