package io.dwg.test;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Analyzes BadObjSize failures - objects with invalid (negative or zero) sizes.
 * This helps identify bit-stream reading alignment issues.
 */
public class AnalyzeBadObjSize {
    static class BadObjInfo {
        int typeCode;
        int objSize;
        long offset;
        String typeName;

        BadObjInfo(int typeCode, int objSize, long offset, String typeName) {
            this.typeCode = typeCode;
            this.objSize = objSize;
            this.offset = offset;
            this.typeName = typeName;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing BadObjSize Issues in Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        System.out.printf("File: Arc.dwg (Version: %s)\n", version);
        System.out.printf("File size: %d bytes\n\n", data.length);

        List<BadObjInfo> badObjs = new ArrayList<>();

        // Scan for objects with bad sizes
        // Objects start around offset 0x4680 based on previous debug output
        int startOffset = 0x4680;
        int maxOffset = data.length - 10;

        ByteBufferBitInput globalBuf = new ByteBufferBitInput(data);

        try {
            for (int offset = startOffset; offset < maxOffset; offset += 2) {
                try {
                    globalBuf.seek((long) offset * 8L);
                    BitStreamReader r = new BitStreamReader(globalBuf, version);

                    int objSize = r.readModularShort();
                    if (objSize <= 0 || objSize > 100000) {
                        // Found a bad size
                        try {
                            int typeCode = r.readBitShort();
                            String typeName = getTypeName(typeCode);
                            badObjs.add(new BadObjInfo(typeCode, objSize, offset, typeName));
                        } catch (Exception e) {
                            // Couldn't read type code
                        }
                    }
                } catch (Exception e) {
                    // Skip errors
                }
            }
        } catch (Exception e) {
            // End of scan
        }

        if (badObjs.isEmpty()) {
            System.out.println("No BadObjSize issues found (all sizes valid)");
        } else {
            System.out.printf("Found %d objects with bad sizes:\n", badObjs.size());
            System.out.println("───────────────────────────────────────────────────────────────");
            System.out.printf("%-15s %-10s %-10s %-10s\n", "Type", "ObjSize", "Offset", "Hex Offset");
            System.out.println("───────────────────────────────────────────────────────────────");

            Map<String, Integer> typeCount = new TreeMap<>();
            for (BadObjInfo info : badObjs) {
                System.out.printf("%-15s %-10d 0x%-8X 0x%-8X\n",
                    info.typeName, info.objSize, info.offset, info.offset);
                typeCount.merge(info.typeName, 1, Integer::sum);
            }

            System.out.println("\n───────────────────────────────────────────────────────────────");
            System.out.println("Summary by Type:");
            typeCount.forEach((type, count) ->
                System.out.printf("  %s: %d objects\n", type, count)
            );
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    static String getTypeName(int typeCode) {
        return switch(typeCode) {
            case 0x00 -> "UNUSED";
            case 0x01 -> "TEXT";
            case 0x04 -> "SEQEND";
            case 0x05 -> "ENDBLK";
            case 0x07 -> "INSERT";
            case 0x11 -> "ARC";
            case 0x12 -> "CIRCLE";
            case 0x13 -> "LINE";
            case 0x2A -> "DICTIONARY";
            case 0x2D -> "LEADER";
            case 0x32 -> "LTYPE";
            case 0x33 -> "LAYER";
            case 0x34 -> "STYLE";
            case 0x35 -> "STYLE_ALT";
            case 0x38 -> "LTYPE_CONTROL";
            case 0x39 -> "LTYPE";
            case 0x3C -> "GROUP";
            case 0x3D -> "MLINESTYLE";
            case 0x3E -> "OLE2FRAME";
            case 0x40 -> "LONG_TRANSACTION";
            case 0x4E -> "PLACEHOLDER";
            case 0x4F -> "VBA_PROJECT";
            case 0x50 -> "LAYOUT";
            default -> String.format("0x%02X", typeCode);
        };
    }
}
