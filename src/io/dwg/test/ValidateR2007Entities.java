package io.dwg.test;

import java.io.*;
import java.nio.file.Files;
import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;

public class ValidateR2007Entities {
    public static void main(String[] args) throws Exception {
        String[] workingFiles = {
            "Arc", "Constraints", "ConstructionLine", "Donut", "Ellipse",
            "Leader", "Multiline", "Point", "RAY", "Spline"
        };
        
        System.out.println("=== R2007 Entity Parsing Validation ===\n");
        
        int totalEntities = 0;
        int successCount = 0;
        
        for (String name : workingFiles) {
            File f = new File("samples/2007/" + name + ".dwg");
            if (!f.exists()) {
                System.out.println("✗ " + name + ".dwg - FILE NOT FOUND");
                continue;
            }
            
            try {
                DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
                int entityCount = doc.entities().size();
                int layerCount = doc.layers().size();
                int linetypeCount = doc.linetypes().size();
                
                System.out.println("✓ " + String.format("%-20s", name + ".dwg") + 
                    " entities=" + String.format("%4d", entityCount) + 
                    " layers=" + String.format("%3d", layerCount) + 
                    " linetypes=" + String.format("%3d", linetypeCount));
                
                totalEntities += entityCount;
                successCount++;
            } catch (Exception e) {
                System.out.println("✗ " + name + ".dwg - " + e.getClass().getSimpleName() + 
                    ": " + e.getMessage());
            }
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("Files parsed: " + successCount + "/" + workingFiles.length);
        System.out.println("Total entities: " + totalEntities);
        if (successCount > 0) {
            System.out.println("Average per file: " + (totalEntities / successCount));
        }
    }
}
