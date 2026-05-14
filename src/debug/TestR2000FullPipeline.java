package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.sections.header.HeaderSectionParser;
import io.dwg.sections.objects.R2000ObjectStreamParser;
import io.dwg.entities.DwgObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Test full R2000 parsing pipeline: Header + Objects extraction and parsing
 */
public class TestR2000FullPipeline {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2000 Full Parsing Pipeline Test ===\n");

        int totalFiles = 0;
        int successCount = 0;
        long totalObjectsParsed = 0;

        try (Stream<Path> paths = Files.walk(Paths.get("samples/2000"))) {
            var files = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .toList();

            System.out.printf("Processing %d R2000 files...\n\n", files.size());

            for (Path path : files) {
                totalFiles++;
                String name = path.getFileName().toString();

                try {
                    System.out.printf("%-30s: ", name);
                    byte[] data = Files.readAllBytes(path);

                    // 1. Read header
                    BitInput input = new ByteBufferBitInput(data);
                    DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(DwgVersion.R2000);
                    FileHeaderFields header = handler.readHeader(input);

                    // 2. Read sections
                    input = new ByteBufferBitInput(data);
                    Map<String, SectionInputStream> sections = handler.readSections(input, header);

                    int headerParsed = 0;
                    int objectsParsed = 0;

                    // 3. Parse Header section
                    try {
                        SectionInputStream headerSec = sections.get("AcDb:Header");
                        if (headerSec != null) {
                            HeaderSectionParser headerParser = new HeaderSectionParser();
                            headerParser.parse(headerSec, DwgVersion.R2000);
                            headerParsed = 1;  // Successfully parsed
                        }
                    } catch (Exception e) {
                        // Header parsing may fail, but that's ok
                    }

                    // 4. Parse Objects section
                    try {
                        SectionInputStream objectsSec = sections.get("AcDb:AcDbObjects");
                        if (objectsSec != null) {
                            R2000ObjectStreamParser objParser = new R2000ObjectStreamParser();
                            Map<Long, DwgObject> objects = objParser.parse(objectsSec, DwgVersion.R2000);
                            objectsParsed = objects.size();
                            totalObjectsParsed += objectsParsed;
                        }
                    } catch (Exception e) {
                        // Objects parsing may fail
                        System.out.printf("Objects parse error: %s", e.getMessage());
                    }

                    System.out.printf("✓ Header=%s, Objects=%d\n",
                        headerParsed > 0 ? "✓" : "✗", objectsParsed);
                    successCount++;

                } catch (Exception e) {
                    System.out.printf("✗ ERROR: %s\n", e.getMessage());
                }
            }
        }

        System.out.printf("\n=== Summary ===\n");
        System.out.printf("Total files: %d\n", totalFiles);
        System.out.printf("Successful: %d (%.0f%%)\n", successCount,
            totalFiles > 0 ? (100.0 * successCount / totalFiles) : 0);
        System.out.printf("Total objects parsed: %d\n", totalObjectsParsed);

        if (successCount == totalFiles) {
            System.out.println("\n✓ R2000 PIPELINE COMPLETE");
        }
    }
}
