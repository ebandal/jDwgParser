import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.DwgSpline;

import java.io.File;

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
                    }
                } catch (Exception e) {
                    System.out.printf("  ERROR %s: %s%n", f.getName(), e.getMessage());
                }
            }
        }
    }
}
