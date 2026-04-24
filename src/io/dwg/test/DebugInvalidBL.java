package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugInvalidBL {
    public static void main(String[] args) throws Exception {
        System.out.println("Analyzing Invalid BL Opcode 11 errors\n");
        
        try {
            byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
            var doc = DwgReader.defaultReader().open(data);
            
            int validCount = 0;
            int invalidCount = 0;
            int exceptionCount = 0;
            
            for (var obj : doc.objectMap().values()) {
                if (obj != null) {
                    String type = obj.getClass().getSimpleName();
                    if (!type.equals("DwgUnknown")) {
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                }
            }
            
            System.out.printf("Total objects: %d%n", doc.objectMap().size());
            System.out.printf("Valid types: %d%n", validCount);
            System.out.printf("Unknown/Invalid: %d%n", invalidCount);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
