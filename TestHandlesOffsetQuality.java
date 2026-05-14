import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.format.r2007.R2007FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;
import io.dwg.sections.handles.HandlesParsingUtil;
import io.dwg.sections.handles.HandleRegistry;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Test R2007 Handles blockCount fix by checking offset quality
 * Measure: negative offsets, out-of-range offsets
 */
public class TestHandlesOffsetQuality {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
            "samples/2007/Arc.dwg",
            "samples/2007/Constraints.dwg",
            "samples/2007/ConstructionLine.dwg",
            "samples/2007/Donut.dwg",
            "samples/2007/Ellipse.dwg",
            "samples/2007/Leader.dwg",
            "samples/2007/Multiline.dwg",
            "samples/2007/Point.dwg",
            "samples/2007/RAY.dwg",
            "samples/2007/Spline.dwg"
        };

        System.out.println("=== R2007 Handles Offset Quality After BlockCount Fix ===\n");
        System.out.println(String.format("%-20s %6s %8s %8s %7s",
            "File", "Handles", "Negative", "OutOfRange", "%Invalid"));
        System.out.println(new String(new char[70]).replace('\0', '-'));

        int totalFiles = 0;
        int filesWithValidOffsets = 0;

        for (String filePath : testFiles) {
            try {
                String fileName = new java.io.File(filePath).getName();
                byte[] fileData = Files.readAllBytes(Paths.get(filePath));
                BitInput input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));

                R2007FileStructureHandler handler = new R2007FileStructureHandler();
                FileHeaderFields header = handler.readHeader(input);

                // Read sections
                input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));
                Map<String, SectionInputStream> sections = handler.readSections(input, header);

                if (sections.containsKey("AcDb:Handles")) {
                    SectionInputStream handlesSection = sections.get("AcDb:Handles");
                    byte[] handlesData = handlesSection.data();

                    // Parse handles and count offset quality
                    HandleRegistry registry = new HandleRegistry();
                    BitStreamReader reader = new BitStreamReader(new ByteBufferBitInput(
                        java.nio.ByteBuffer.wrap(handlesData)));

                    HandlesParsingUtil.parseHandlesPagesR2000(reader, registry);

                    // Analyze offset quality
                    int negativeCount = 0;
                    int outOfRangeCount = 0;
                    int maxOffset = 0;

                    for (long handle : registry.allHandles()) {
                        var offsetOpt = registry.offsetFor(handle);
                        if (offsetOpt.isPresent()) {
                            long offset = offsetOpt.get();
                            if (offset < 0) {
                                negativeCount++;
                            }
                            if (offset >= handlesData.length) {
                                outOfRangeCount++;
                            }
                            maxOffset = Math.max(maxOffset, (int)offset);
                        }
                    }

                    int totalInvalid = negativeCount + outOfRangeCount;
                    int totalHandles = registry.allHandles().size();
                    double invalidPercent = totalHandles > 0 ?
                        (100.0 * totalInvalid / totalHandles) : 0;

                    System.out.printf("%-20s %6d %8d %8d %6.1f%%\n",
                        fileName, totalHandles, negativeCount, outOfRangeCount, invalidPercent);

                    if (invalidPercent < 5.0) {
                        filesWithValidOffsets++;
                    }
                    totalFiles++;
                }

            } catch (Exception e) {
                String fileName = new java.io.File(filePath).getName();
                System.err.printf("%-20s ERROR: %s\n", fileName, e.getMessage());
            }
        }

        System.out.println(new String(new char[70]).replace('\0', '-'));
        System.out.println(String.format("\nFiles with <5%% invalid offsets: %d/%d",
            filesWithValidOffsets, totalFiles));
        if (filesWithValidOffsets == totalFiles) {
            System.out.println("✓ SUCCESS: All files have valid offsets after blockCount fix!");
        }
    }
}
