package run;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import decode.ObjectScannerR2000;
import structure.DwgObject;

/**
 * Lists the objects of R2000 (AC1015) files by type.
 * Usage: ObjectStats [file or directory]   (default: AC1015)
 */
public class ObjectStats {

    public static void main(String[] args) {
        File target = new File(args.length > 0 ? args[0] : "AC1015");
        File[] files = target.isDirectory()
                ? target.listFiles((d, n) -> n.toLowerCase().endsWith(".dwg"))
                : new File[] { target };
        if (files == null || files.length == 0) {
            System.out.println("no .dwg file in " + target);
            return;
        }
        Arrays.sort(files);

        Map<String, int[]> total = new TreeMap<>();
        for (File f : files) {
            ObjectScannerR2000 scanner = new ObjectScannerR2000();
            try {
                scanner.scan(f);
            } catch (Exception e) {
                System.out.println("== " + f.getName() + " : FAILED (" + e + ")");
                continue;
            }

            Map<String, int[]> counts = new TreeMap<>();
            int entities = 0;
            for (DwgObject o : scanner.objects) {
                String key = (o.entity ? "E " : "O ") + o.typeName;
                counts.computeIfAbsent(key, k -> new int[1])[0]++;
                total.computeIfAbsent(key, k -> new int[1])[0]++;
                if (o.entity) {
                    entities++;
                }
            }

            System.out.println("== " + f.getName());
            System.out.println("   map entries=" + scanner.objectMap.size()
                    + ", objects read=" + scanner.objects.size()
                    + " (entities=" + entities + ")"
                    + ", classes=" + scanner.classes.size()
                    + ", handle mismatches=" + scanner.handleMismatches
                    + ", failures=" + scanner.failures);
            print(counts);
        }

        if (files.length > 1) {
            System.out.println("== TOTAL (" + files.length + " files)");
            print(total);
        }
    }

    // "E" = entity, "O" = non-graphical object
    private static void print(Map<String, int[]> counts) {
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .forEach(e -> System.out.printf("   %7d  %s%n", e.getValue()[0], e.getKey()));
    }
}
