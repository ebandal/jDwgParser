import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.concrete.DwgSpline;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * Targeted diagnostic for SPLINE failures in R2004 and R2010.
 */
public class DiagnoseSpline {

    public static void main(String[] args) throws Exception {
        String[] versionDirs = {"DWG/2004", "DWG/2010", "DWG/2013"};

        for (String vdir : versionDirs) {
            File dir = new File(vdir);
            if (!dir.exists()) continue;
            File[] files = dir.listFiles(f -> f.getName().endsWith(".dwg"));
            if (files == null) continue;

            System.out.println("\n=== " + vdir + " ===");
            for (File f : files) {
                try {
                    DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
                    if (doc == null) continue;
                    int totalSpline = 0;
                    int okSpline = 0;
                    for (DwgObject obj : doc.objectMap().values()) {
                        if (obj instanceof DwgSpline sp) {
                            totalSpline++;
                            boolean ok = !sp.controlPoints().isEmpty() || !sp.fitPoints().isEmpty();
                            if (ok) okSpline++;
                            else {
                                System.out.printf("  FAIL: %s degree=%d ctrlPts=%d fitPts=%d%n",
                                    f.getName(), sp.degree(), sp.controlPoints().size(), sp.fitPoints().size());
                            }
                        }
                    }
                    if (totalSpline > 0) {
                        System.out.printf("  %s: %d/%d SPLINE OK%n", f.getName(), okSpline, totalSpline);
                    } else if (f.getName().contains("Spline") || f.getName().contains("spline")) {
                        System.out.printf("  %s: 0 SPLINE entities found in objectMap (size=%d)%n",
                            f.getName(), doc.objectMap().size());
                        // Print type code breakdown to understand what we DO find
                        Map<Integer, Integer> typeCounts = new TreeMap<>();
                        for (DwgObject obj : doc.objectMap().values()) {
                            int code = obj instanceof AbstractDwgObject ao ? ao.rawTypeCode() : -1;
                            typeCounts.merge(code, 1, Integer::sum);
                        }
                        typeCounts.forEach((code, count) ->
                            System.out.printf("    typeCode=0x%02X (%s) x%d%n",
                                code, DwgObjectType.fromCode(code), count));
                    }
                } catch (Exception e) {
                    System.out.printf("  ERROR %s: %s%n", f.getName(), e.getMessage());
                }
            }
        }
    }
}
