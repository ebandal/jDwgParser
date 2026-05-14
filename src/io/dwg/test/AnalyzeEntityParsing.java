package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.DwgObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Analyze current entity parsing status.
 */
public class AnalyzeEntityParsing {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Entity Parsing Analysis");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        var doc = DwgReader.defaultReader().open(data);

        Map<String, Integer> entityTypes = new TreeMap<>();
        Map<String, Integer> nonEntityTypes = new TreeMap<>();
        int totalEntities = 0;
        int totalNonEntities = 0;

        for (DwgObject obj : doc.objectMap().values()) {
            if (obj instanceof DwgEntity) {
                DwgEntity entity = (DwgEntity) obj;
                totalEntities++;
                String type = entity.objectType().toString();
                entityTypes.merge(type, 1, Integer::sum);
            } else {
                totalNonEntities++;
                String type = obj.objectType().toString();
                nonEntityTypes.merge(type, 1, Integer::sum);
            }
        }

        System.out.printf("Total objects: %d\n", doc.objectMap().size());
        System.out.printf("Total entities: %d\n", totalEntities);
        System.out.printf("Total non-entities: %d\n\n", totalNonEntities);

        System.out.println("Entity Types (Top 15):");
        entityTypes.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(15)
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.println("\nNon-Entity Types (Top 15):");
        nonEntityTypes.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(15)
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
