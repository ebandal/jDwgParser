package io.dwg.test;

import java.io.*;
import io.dwg.api.DwgReader;
import io.dwg.core.io.SectionInputStream;

public class CompareHandlesBytes {
    public static void main(String[] args) throws Exception {
        String[] files = {"Constraints", "Arc"};
        
        for (String name : files) {
            File f = new File("samples/2007/" + name + ".dwg");
            if (!f.exists()) continue;
            
            System.out.println("\n=== " + name + ".dwg Handles Section (first 64 bytes) ===");
            
            // Extract by reading file directly and looking for Handles section
            byte[] fullData = new byte[(int)f.length()];
            try (FileInputStream fis = new FileInputStream(f)) {
                fis.read(fullData);
            }
            
            // This is a simplified view - in reality Handles is extracted via R2007FileStructureHandler
            System.out.println("(Handles section is extracted via R2007FileStructureHandler from compressed data)");
            System.out.println("This requires examining during actual parsing...");
        }
    }
}
