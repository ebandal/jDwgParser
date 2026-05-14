package run;

import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.objects.ObjectsSectionParser;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.classes.ClassesSectionParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Diagnose why objectMap is empty in DwgDocument
 */
public class ObjectMapDiagnosticTest {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
                "samples/example_2004.dwg",
                "samples/example_2018.dwg"
        };

        for (String filePath : testFiles) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("File: " + filePath);
            System.out.println("=".repeat(80));

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.out.println("❌ File not found: " + path.toAbsolutePath());
                continue;
            }

            try {
                byte[] data = Files.readAllBytes(path);
                System.out.println("✓ File loaded: " + data.length + " bytes");

                // Step 1: Detect version
                DwgVersion version = DwgVersionDetector.detect(data);
                System.out.println("✓ Version detected: " + version);

                // Step 2: Create document
                DwgDocument doc = new DwgDocument(version);

                // Step 3: Get format handler
                DwgFileStructureHandler handler =
                    DwgFileStructureHandlerFactory.forVersion(version);
                System.out.println("✓ Handler created: " + handler.getClass().getSimpleName());

                // Step 4: Read header
                BitInput input = new ByteBufferBitInput(data);
                FileHeaderFields headerFields = handler.readHeader(input);
                System.out.println("✓ Header read successfully");

                // Step 5: Read sections
                input = new ByteBufferBitInput(data);
                Map<String, SectionInputStream> sections =
                    handler.readSections(input, headerFields);

                System.out.println("✓ Sections extracted: " + sections.size());
                System.out.println("  Available sections:");
                sections.keySet().forEach(name ->
                    System.out.println("    - " + name)
                );

                // Step 6: Check Objects section
                SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
                if (objectsStream == null) {
                    System.out.println("❌ Objects section NOT FOUND!");
                    System.out.println("   Available keys: " + sections.keySet());
                } else {
                    System.out.println("✓ Objects section found");
                    System.out.println("  Stream size: " + objectsStream.size() + " bytes");

                    // Try to parse objects
                    HandleRegistry handleRegistry = new HandleRegistry();
                    SectionInputStream handlesStream = sections.get("AcDb:Handles");
                    if (handlesStream != null) {
                        handleRegistry = new HandlesSectionParser().parse(handlesStream, version);
                        System.out.println("✓ Handles parsed: " + handleRegistry.toString());
                    }

                    DwgClassRegistry classRegistry = new DwgClassRegistry();
                    SectionInputStream classStream = sections.get("AcDb:Classes");
                    if (classStream != null) {
                        ClassesSectionParser classParser = new ClassesSectionParser();
                        classParser.parse(classStream, version)
                            .forEach(classRegistry::register);
                        System.out.println("✓ Classes parsed");
                    }

                    // Parse objects
                    ObjectsSectionParser objParser = new ObjectsSectionParser();
                    objParser.setHandleRegistry(handleRegistry);
                    objParser.setClassRegistry(classRegistry);
                    Map<Long, ?> objectMap = objParser.parse(objectsStream, version);

                    System.out.println("✓ Objects parsed: " + objectMap.size() + " objects");
                    if (objectMap.isEmpty()) {
                        System.out.println("❌ ObjectMap is EMPTY after parsing!");
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ ERROR: " + e.getClass().getSimpleName());
                System.out.println("   Message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Diagnosis complete");
        System.out.println("=".repeat(80));
    }
}
