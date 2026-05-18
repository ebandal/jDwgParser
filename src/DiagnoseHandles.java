import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.*;

import java.io.File;
import java.util.*;

/**
 * Diagnose entity geometry failures per DWG version directory.
 */
public class DiagnoseHandles {

    public static void main(String[] args) throws Exception {
        String[] versionDirs = {"DWG/2000", "DWG/2004", "DWG/2007", "DWG/2010", "DWG/2013", "DWG/2018", "DWG/r14"};

        System.out.printf("%-12s %-10s %6s %6s%n", "Type", "Version", "OK", "FAIL");
        System.out.println("-".repeat(40));

        for (String vdir : versionDirs) {
            File dir = new File(vdir);
            if (!dir.exists()) continue;
            File[] files = dir.listFiles(f -> f.getName().endsWith(".dwg"));
            if (files == null) continue;

            Map<String, int[]> counts = new LinkedHashMap<>();

            for (File f : files) {
                try {
                    DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
                    if (doc == null) continue;
                    for (DwgObject obj : doc.objectMap().values()) {
                        if (obj instanceof DwgFace3D face) {
                            boolean ok = face.points() != null && face.points()[0] != null;
                            counts.computeIfAbsent("FACE3D", k -> new int[2])[ok ? 0 : 1]++;
                        } else if (obj instanceof DwgLwPolyline lw) {
                            boolean ok = lw.vertexCount() > 0;
                            counts.computeIfAbsent("LWPLINE", k -> new int[2])[ok ? 0 : 1]++;
                        } else if (obj instanceof DwgSpline sp) {
                            boolean ok = !sp.controlPoints().isEmpty() || !sp.fitPoints().isEmpty();
                            counts.computeIfAbsent("SPLINE", k -> new int[2])[ok ? 0 : 1]++;
                        } else if (obj instanceof DwgMText mt) {
                            boolean ok = mt.location() != null;
                            counts.computeIfAbsent("MTEXT", k -> new int[2])[ok ? 0 : 1]++;
                        } else if (obj instanceof DwgAttdef ad) {
                            boolean ok = ad.insertionPoint() != null;
                            counts.computeIfAbsent("ATTDEF", k -> new int[2])[ok ? 0 : 1]++;
                        } else if (obj instanceof DwgCircle) {
                            counts.computeIfAbsent("CIRCLE", k -> new int[2])[0]++;
                        } else if (obj instanceof DwgLine) {
                            counts.computeIfAbsent("LINE", k -> new int[2])[0]++;
                        }
                    }
                } catch (Exception e) {
                    // skip file-level errors
                }
            }

            String ver = vdir.replace("DWG/", "");
            for (var e : counts.entrySet()) {
                System.out.printf("%-12s %-10s %6d %6d%n",
                    e.getKey(), ver, e.getValue()[0], e.getValue()[1]);
            }
        }
    }
}
