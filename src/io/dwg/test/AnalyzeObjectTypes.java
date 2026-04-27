package io.dwg.test;

import io.dwg.api.DwgReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AnalyzeObjectTypes {
    public static void main(String[] args) throws Exception {
        System.out.println("Analyzing R2000 Object Types\n");
        
        try {
            byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
            var doc = DwgReader.defaultReader().open(data);
            System.out.printf("\nParsed %d objects%n", doc.objectMap().size());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
