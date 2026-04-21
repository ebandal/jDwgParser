package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class DebugR2000DetailedOffsets {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        System.out.printf("Total file size: 0x%X (%d bytes)\n\n", data.length, data.length);

        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        System.out.println("=== After readHeader() ===\n");

        Map<String, Long> offsets = header.sectionOffsets();
        Map<String, Long> sizes = header.sectionSizes();

        if (offsets == null) {
            System.out.println("offsets is NULL!");
        } else {
            System.out.printf("offsets has %d entries:\n", offsets.size());
            for (String key : offsets.keySet()) {
                long offset = offsets.get(key);
                long size = sizes.get(key);
                System.out.printf("  %s:\n", key);
                System.out.printf("    offset=0x%X (%d)\n", offset, offset);
                System.out.printf("    size=0x%X (%d)\n", size, size);
                if (size == -1L) {
                    System.out.println("    [SPECIAL] size=-1 (read until sentinel)");
                }
            }
        }

        // Dump first and last 256 bytes of each section
        System.out.println("\n=== Section Content Verification ===\n");

        long headerOffset = offsets.get("AcDb:Header");
        long headerSize = sizes.get("AcDb:Header");
        System.out.printf("Header @ 0x%X (size 0x%X):\n", headerOffset, headerSize);
        System.out.print("  First 32 bytes: ");
        for (int i = 0; i < 32 && headerOffset + i < data.length; i++) {
            System.out.printf("%02X ", data[(int)(headerOffset + i)] & 0xFF);
        }
        System.out.println();

        long objOffset = offsets.get("AcDb:AcDbObjects");
        System.out.printf("\nObjects @ 0x%X:\n", objOffset);
        System.out.print("  First 32 bytes: ");
        for (int i = 0; i < 32 && objOffset + i < data.length; i++) {
            System.out.printf("%02X ", data[(int)(objOffset + i)] & 0xFF);
        }
        System.out.println();

        // Find sentinel markers (16 consecutive zeros)
        System.out.println("\n=== Sentinel Marker Analysis ===\n");
        findSentinels(data);
    }

    static void findSentinels(byte[] data) {
        System.out.println("Looking for 16 consecutive zero bytes...");
        int count = 0;
        for (int i = 0; i <= data.length - 16; i++) {
            boolean isSentinel = true;
            for (int j = 0; j < 16; j++) {
                if (data[i + j] != 0) {
                    isSentinel = false;
                    break;
                }
            }
            if (isSentinel) {
                count++;
                if (count <= 5) {  // Show first 5
                    System.out.printf("  Sentinel #%d @ 0x%X\n", count, i);
                }
            }
        }
        System.out.printf("Total sentinels found: %d\n", count);
    }
}
