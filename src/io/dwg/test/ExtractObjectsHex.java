package io.dwg.test;

import java.io.*;
import java.nio.ByteBuffer;

public class ExtractObjectsHex {
    public static void main(String[] args) throws Exception {
        String[] files = {"Constraints", "Arc", "Ellipse", "Leader"};
        
        for (String name : files) {
            File dwgFile = new File("samples/2007/" + name + ".dwg");
            if (!dwgFile.exists()) continue;
            
            try {
                // Simple extraction: find Objects section markers
                byte[] fullData = new byte[(int)dwgFile.length()];
                try (FileInputStream fis = new FileInputStream(dwgFile)) {
                    fis.read(fullData);
                }
                
                System.out.println("\n" + name + ".dwg (" + fullData.length + " bytes):");
                System.out.println("First 128 bytes after header (0x400+):");
                
                int offset = 0x400 + 1024; // Skip header + PageMap area
                for (int i = 0; i < 128 && offset + i < fullData.length; i += 16) {
                    System.out.printf("  0x%04X: ", i);
                    for (int j = i; j < i + 16 && offset + j < fullData.length; j++) {
                        System.out.printf("%02X ", fullData[offset + j] & 0xFF);
                    }
                    System.out.println();
                }
                
            } catch (Exception e) {
                System.out.println(name + ": ERROR - " + e.getMessage());
            }
        }
    }
}
