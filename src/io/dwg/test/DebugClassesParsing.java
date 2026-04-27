package io.dwg.test;

import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Debug Classes section parsing for R2000 files.
 */
public class DebugClassesParsing {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging Classes Section Parsing (Arc.dwg)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        System.out.printf("File version: %s\n", version);
        System.out.printf("File size: %d bytes\n\n", data.length);

        // Classes section is at offset 0x6D91 with size 1048
        int classesOffset = 0x6D91;
        int classesSize = 1048;

        System.out.printf("Classes section: offset=0x%X (%d), size=%d bytes\n",
            classesOffset, classesOffset, classesSize);
        System.out.println("───────────────────────────────────────────────────────────────\n");

        try {
            // Extract classes section
            byte[] classesData = new byte[classesSize];
            System.arraycopy(data, classesOffset, classesData, 0, classesSize);

            System.out.println("First 32 bytes (hex):");
            for (int i = 0; i < Math.min(32, classesData.length); i++) {
                System.out.printf("%02X ", classesData[i] & 0xFF);
                if ((i + 1) % 16 == 0) System.out.println();
            }
            System.out.println("\n");

            // Try to parse with SectionInputStream
            System.out.println("Attempting to parse with SectionInputStream...");
            SectionInputStream classesStream = new SectionInputStream(classesData, "AcDb:Classes");

            System.out.println("Creating ClassesSectionParser...");
            ClassesSectionParser parser = new ClassesSectionParser();

            System.out.println("Parsing classes...");
            List<DwgClassDefinition> classes = parser.parse(classesStream, version);

            System.out.printf("\n✅ Classes parsed successfully: %d classes\n", classes.size());
            System.out.println("───────────────────────────────────────────────────────────────");

            if (!classes.isEmpty()) {
                System.out.println("\nFirst 5 classes:");
                for (int i = 0; i < Math.min(5, classes.size()); i++) {
                    DwgClassDefinition cls = classes.get(i);
                    System.out.printf("  [%d] num=%d, dxfName=%s, cppName=%s\n", i,
                        cls.classNumber(), cls.dxfRecordName(), cls.cppClassName());
                }
            }

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            System.out.println("───────────────────────────────────────────────────────────────");
            e.printStackTrace();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
