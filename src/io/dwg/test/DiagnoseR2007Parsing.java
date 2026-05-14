package io.dwg.test;

import java.io.*;
import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;

public class DiagnoseR2007Parsing {
    public static void main(String[] args) throws Exception {
        File f = new File("samples/2007/Arc.dwg");
        
        System.out.println("=== Diagnosing R2007 Parsing ===\n");
        System.out.println("File: " + f.getAbsolutePath());
        System.out.println("Exists: " + f.exists());
        System.out.println("Size: " + f.length() + " bytes");
        
        DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
        
        System.out.println("\nDocument Information:");
        System.out.println("- Version: " + doc.version());
        System.out.println("- Entities: " + doc.entities().size());
        System.out.println("- Layers: " + doc.layers().size());
        System.out.println("- Linetypes: " + doc.linetypes().size());
        System.out.println("- Custom classes: " + doc.customClasses().size());
        System.out.println("- Object map size: " + doc.objectMap().size());
        System.out.println("- Handle registry: " + (doc.handleRegistry() != null ? "OK" : "NULL"));
        System.out.println("- Class registry: " + (doc.classRegistry() != null ? "OK" : "NULL"));
        
        if (doc.objectMap().size() > 0) {
            System.out.println("\nFirst 5 objects in map:");
            doc.objectMap().keySet().stream().limit(5).forEach(handle -> {
                System.out.println("  Handle: " + Long.toHexString(handle) + 
                    " Type: " + doc.objectMap().get(handle).getClass().getSimpleName());
            });
        }
    }
}
