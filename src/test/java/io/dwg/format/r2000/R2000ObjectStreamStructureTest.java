package test.java.io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.version.DwgVersion;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import io.dwg.core.io.SectionInputStream;

/**
 * Analyze the actual binary structure of R2000 object stream
 */
public class R2000ObjectStreamStructureTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2000 Object Stream Binary Structure Analysis");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/example_2000.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        DwgVersion version = DwgVersion.R2000;
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);

        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            System.out.println("ERROR: Objects section not found!");
            return;
        }

        java.lang.reflect.Field field = objectsStream.getClass().getDeclaredField("rawData");
        field.setAccessible(true);
        byte[] rawData = (byte[]) field.get(objectsStream);

        System.out.println("Object stream size: " + rawData.length + " bytes");
        System.out.println();

        // Check for sentinels like R13
        System.out.println("Checking for 16-byte sentinel at start (like R13):");
        boolean hasSentinelStart = true;
        for (int i = 0; i < 16 && i < rawData.length; i++) {
            if (rawData[i] != 0) {
                hasSentinelStart = false;
                break;
            }
        }
        if (hasSentinelStart) {
            System.out.println("  ✓ Found 16-byte zero sentinel at offset 0x0");
        } else {
            System.out.println("  ✗ No 16-byte sentinel at offset 0x0");
            System.out.print("    First 16 bytes: ");
            for (int i = 0; i < 16 && i < rawData.length; i++) {
                System.out.printf("%02X ", rawData[i] & 0xFF);
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Analyzing first 3 potential objects:");
        System.out.println("-".repeat(80));

        int offset = 0;

        // Try with R13 sentinel structure: skip 16-byte sentinel
        if (hasSentinelStart) {
            offset = 16;
            System.out.println("(Skipping 16-byte sentinel at start)");
            System.out.println();
        }

        for (int objNum = 0; objNum < 3 && offset < rawData.length - 20; objNum++) {
            System.out.printf("Object [%d] at offset 0x%X:\n", objNum, offset);

            // Try reading as modular short (MS) - 2 bytes
            int ms = readModularShort(rawData, offset);
            System.out.printf("  Modular Short (objSize): 0x%X (%d bytes)\n", ms, ms);

            // Next is bit short (BS) - 2 bytes
            int bs = readBitShort(rawData, offset + 2);
            System.out.printf("  Bit Short (typeCode): 0x%X (%d)\n", bs, bs);

            // Show raw bytes
            System.out.print("  Raw bytes: ");
            for (int i = 0; i < Math.min(32, rawData.length - offset); i++) {
                System.out.printf("%02X ", rawData[offset + i] & 0xFF);
            }
            System.out.println();

            offset += Math.max(4, Math.min(ms + 2, 100));
            System.out.println();
        }

        System.out.println();
        System.out.println("=".repeat(80));
    }

    private static int readModularShort(byte[] data, int offset) {
        if (offset + 1 >= data.length) return -1;
        int b1 = data[offset] & 0xFF;
        int b2 = data[offset + 1] & 0xFF;
        // Modular short: if first byte < 0xF0, it's (b1 << 8) | b2
        // Otherwise special handling
        return (b1 << 8) | b2;
    }

    private static int readBitShort(byte[] data, int offset) {
        if (offset + 1 >= data.length) return -1;
        int b1 = data[offset] & 0xFF;
        int b2 = data[offset + 1] & 0xFF;
        return (b1 << 8) | b2;
    }
}
