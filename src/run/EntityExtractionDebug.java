package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.sections.objects.ObjectsSectionParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entity 추출 디버그 - Object Stream 분석
 */
public class EntityExtractionDebug {
    public static void main(String[] args) throws Exception {
        String file = "samples/2004/Circle.dwg";
        Path path = Paths.get(file);

        System.out.println("파일: " + file);
        System.out.println("크기: " + Files.size(path) + " bytes");

        byte[] data = Files.readAllBytes(path);

        // DwgDocument 만들기 (이미 섹션들이 파싱됨)
        DwgDocument doc = DwgReader.defaultReader().open(data);

        System.out.println("Version: " + doc.version());
        System.out.println("Objects in map: " + (doc.objectMap() != null ? doc.objectMap().size() : 0));

        if (doc.objectMap() != null && doc.objectMap().size() > 0) {
            System.out.println("\nObject details:");
            doc.objectMap().forEach((handle, obj) -> {
                System.out.printf("  Handle 0x%X: %s\n", handle, obj.getClass().getSimpleName());
            });
        }

        // 이제 내부적으로 Object Stream을 다시 추출하고 분석
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Object Stream Manual Analysis:");
        System.out.flush();

        io.dwg.core.io.BitInput input = new ByteBufferBitInput(data);
        io.dwg.core.version.DwgVersionDetector.detect(data);
        io.dwg.format.common.DwgFileStructureHandler handler =
            io.dwg.format.common.DwgFileStructureHandlerFactory.forVersion(doc.version());
        io.dwg.format.common.FileHeaderFields headerFields = handler.readHeader(input);

        input = new ByteBufferBitInput(data);
        java.util.Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        System.out.println("\nSections map size: " + sections.size());
        System.out.println("Available sections:");
        for (String key : sections.keySet()) {
            System.out.println("  - " + key);
        }
        System.out.flush();

        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            System.out.println("\n⚠️  Objects section not found!");
            System.out.println("Trying alternative keys...");
            for (String key : sections.keySet()) {
                if (key.toLowerCase().contains("object")) {
                    System.out.println("  Found: " + key);
                }
            }
            return;
        }

        byte[] objData = objectsStream.rawBytes();
        System.out.printf("Object Stream size: %d bytes\n\n", objData.length);

        // 처음 1000바이트 분석
        System.out.println("First 30 ModularShort values:");
        ByteBufferBitInput buf = new ByteBufferBitInput(
            java.nio.ByteBuffer.wrap(objData, 0, Math.min(500, objData.length)));
        BitStreamReader reader = new BitStreamReader(buf, doc.version());

        for (int i = 0; i < 30; i++) {
            try {
                if (buf.isEof()) {
                    System.out.println("  EOF reached");
                    break;
                }

                int val = reader.readModularShort();
                System.out.printf("[%2d] 0x%04X (%d)\n", i, val, val);

            } catch (Exception e) {
                System.out.printf("[%2d] Error: %s\n", i, e.getMessage());
                break;
            }
        }

        System.out.println();
        System.out.println("Trying parseStreaming manually:");

        // ObjectsSectionParser의 parseStreaming 로직을 수동으로 실행
        int offset = 0;
        int parsedCount = 0;

        while (offset < objData.length - 4 && parsedCount < 10) {
            try {
                ByteBufferBitInput buf2 = new ByteBufferBitInput(
                    java.nio.ByteBuffer.wrap(objData, offset, objData.length - offset));
                BitStreamReader r = new BitStreamReader(buf2, doc.version());

                int objSize = r.readModularShort();
                System.out.printf("\noffset 0x%04X: objSize=%d (0x%X)\n", offset, objSize, objSize);

                if (objSize <= 0 || objSize > 0x100000) {
                    offset++;
                    continue;
                }

                int typeCode = r.readBitShort();
                System.out.printf("  typeCode=%d (0x%X)\n", typeCode, typeCode);

                if (typeCode < 0 || typeCode > 999) {
                    offset++;
                    continue;
                }

                offset += 2 + objSize;
                parsedCount++;

            } catch (Exception e) {
                System.out.printf("  Error at offset 0x%04X: %s\n", offset, e.getMessage());
                offset++;
            }
        }

        System.out.printf("\nParsed %d objects total\n", parsedCount);
    }
}
