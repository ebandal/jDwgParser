package io.dwg.test;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.core.io.SectionInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Debug R2000 Classes section parsing.
 */
public class DebugClassesParsing {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2000 Classes Section Parsing");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        System.out.printf("File: Arc.dwg, Version: %s\n", version);
        System.out.printf("File size: %d bytes\n\n", data.length);

        // Known Classes offset from earlier debugging
        int classesOffset = 0x6D91;
        System.out.printf("Classes offset (from header): 0x%X\n", classesOffset);
        System.out.printf("Reading first 256 bytes:\n");

        for (int i = 0; i < 256 && classesOffset + i < data.length; i += 16) {
            System.out.printf("  0x%X: ", classesOffset + i);
            for (int j = 0; j < 16 && classesOffset + i + j < data.length; j++) {
                System.out.printf("%02X ", data[classesOffset + i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nAttempting to parse with ClassesSectionParser...");
        try {
            byte[] classesData = new byte[Math.min(2048, data.length - classesOffset)];
            System.arraycopy(data, classesOffset, classesData, 0, classesData.length);

            SectionInputStream stream = new SectionInputStream(classesData, "AcDb:Classes");
            ClassesSectionParser parser = new ClassesSectionParser();
            List<DwgClassDefinition> classes = parser.parse(stream, version);

            System.out.printf("\nClasses parsed: %d\n", classes.size());
            for (int i = 0; i < Math.min(10, classes.size()); i++) {
                DwgClassDefinition cls = classes.get(i);
                System.out.printf("  Class %d: num=%d name=%s\n",
                    i, cls.classNumber(), cls.dxfRecordName());
            }
        } catch (Exception e) {
            System.out.printf("Error parsing: %s\n", e.getMessage());
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
