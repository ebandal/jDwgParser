package io.dwg.test;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.entities.DwgObjectType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Summarize failures by type code for prioritization.
 */
public class FailureSummary {
    static class FailureStats {
        DwgObjectType type;
        int count;
        int validSize;  // Count with valid objSize
        int invalidSize; // Count with invalid objSize

        FailureStats(DwgObjectType type) {
            this.type = type;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Failure Summary by Type (Arc.dwg)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        // Map type code -> failure stats
        Map<DwgObjectType, FailureStats> failureMap = new TreeMap<>((a, b) ->
            Integer.compare(a.typeCode(), b.typeCode())
        );

        // Scan all potential object locations
        ByteBufferBitInput globalBuf = new ByteBufferBitInput(data);
        int successCount = 0;
        int failureCount = 0;

        // Sample known handle offsets from earlier analysis
        int[] sampleOffsets = {
            0x722D, 0x72BE, 0x72E5, 0x8AD4, 0x9767, 0x89CE, 0xA109,
            0x72D6, 0x8A3D, 0x91F3, 0x8850, 0x8838, 0x8868, 0x87F0
        };

        for (int offset : sampleOffsets) {
            try {
                globalBuf.seek((long) offset * 8L);
                BitStreamReader r = new BitStreamReader(globalBuf, version);

                int objSize = r.readModularShort();
                int typeCode = r.readBitShort();
                DwgObjectType type = DwgObjectType.fromCode(typeCode);

                FailureStats stats = failureMap.computeIfAbsent(type, FailureStats::new);
                stats.count++;

                if (objSize > 0 && objSize <= 100000) {
                    stats.validSize++;
                } else {
                    stats.invalidSize++;
                }
            } catch (Exception e) {
                // Skip
            }
        }

        System.out.println("TOP FAILURE TYPES (by frequency):");
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("%-20s %-10s %-10s %-10s\n", "Type", "Count", "Valid", "Invalid");
        System.out.println("───────────────────────────────────────────────────────────────");

        failureMap.values().stream()
            .filter(s -> s.count > 0)
            .sorted((a, b) -> Integer.compare(b.count, a.count))
            .forEach(stats ->
                System.out.printf("%-20s %-10d %-10d %-10d\n",
                    stats.type.toString(), stats.count, stats.validSize, stats.invalidSize)
            );

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("\nRecommendation:");
        System.out.println("- Types with 'Valid' size: Can implement readers to parse");
        System.out.println("- Types with 'Invalid' size: Data corruption, skip gracefully");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
