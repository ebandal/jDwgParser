package test;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import structure.Dwg;
import decode.DwgParseException;

public class TestDwgDecode {
    public static void main(String[] args) throws Exception {
        // Enable fine logging to see ObjectReader activity
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (java.util.logging.Handler h : root.getHandlers()) h.setLevel(Level.FINE);

        String path = args.length > 0 ? args[0] : "samples/2004/Arc.dwg";
        File f = new File(path);
        System.out.println("=== Decoding: " + f.getAbsolutePath() + " ===");

        Dwg dwg = new Dwg();
        try {
            dwg.decode(f);
        } catch (DwgParseException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Version: " + dwg.header.ver);
        System.out.println("VersionId: " + dwg.header.versionId);
        if (dwg.systemSectionPageList != null) {
            System.out.println("System section pages: " + dwg.systemSectionPageList.size());
            for (structure.sectionpage.SystemSectionPage p : dwg.systemSectionPageList) {
                System.out.println("  type=0x" + Integer.toHexString(p.header.type)
                        + " comp=" + p.header.compressedSize
                        + " decomp=" + p.header.decompressedSize
                        + " got=" + (p.decompressedData == null ? 0 : p.decompressedData.length));
            }
        }
        if (dwg.dataSectionPageList != null) {
            System.out.println("Data section pages: " + dwg.dataSectionPageList.size());
            for (structure.sectionpage.DataSectionPage p : dwg.dataSectionPageList) {
                System.out.println("  sec=" + p.header.sectionNumber
                        + " comp=" + p.header.compressDataSize
                        + " decomp=" + p.header.decompressedPageSize
                        + " got=" + (p.decompressedData == null ? 0 : p.decompressedData.length));
            }
        }

        // Entity parsing results
        System.out.println("\n=== Parsed Entities ===");
        System.out.println("parsedObjects is: " + (dwg.parsedObjects == null ? "null" : "not null"));
        if (dwg.parsedObjects != null) {
            System.out.println("parsedObjects size: " + dwg.parsedObjects.size());
        }

        if (dwg.parsedObjects != null && !dwg.parsedObjects.isEmpty()) {
            int totalCount = 0;
            java.util.Map<String, Integer> typeCount = new java.util.HashMap<>();

            for (structure.entities.DwgObject obj : dwg.parsedObjects.values()) {
                if (obj != null) {
                    String typeName = obj.getClass().getSimpleName();
                    typeCount.put(typeName, typeCount.getOrDefault(typeName, 0) + 1);
                    totalCount++;
                }
            }

            System.out.println("Total objects: " + totalCount);
            typeCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
        } else {
            System.out.println("No objects decoded");
        }
    }
}
