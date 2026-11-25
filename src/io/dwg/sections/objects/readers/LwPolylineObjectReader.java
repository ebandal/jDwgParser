package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLwPolyline;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

import java.util.ArrayList;
import java.util.List;

/**
 * LWPOLYLINE (spec §20.4.85 LWPLINE; its type number comes from the classes section).
 * <pre>
 *   flag BS; then, by flag bit:
 *     4 constant width BD, 8 elevation BD, 2 thickness BD, 1 normal 3BD
 *   number of points BL; 16 number of bulges BL; R2010+ 1024 number of vertex ids BL;
 *   32 number of widths BL
 *   points: R13–R14 all 2RD; R2000+ the first 2RD, the others 2DD with the previous point as
 *   default
 *   bulges BD each; R2010+ vertex ids BL each; widths start and end BD each
 * </pre>
 */
public class LwPolylineObjectReader implements ObjectReader {

    private static final int NORMAL = 1;
    private static final int THICKNESS = 2;
    private static final int CONSTANT_WIDTH = 4;
    private static final int ELEVATION = 8;
    private static final int BULGES = 16;
    private static final int WIDTHS = 32;
    private static final int VERTEX_IDS = 1024;

    @Override
    public int objectType() { return DwgObjectType.LWPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgLwPolyline polyline = (DwgLwPolyline) target;

        int flag = r.readBitShort();
        polyline.setFlags(flag);
        if (has(flag, CONSTANT_WIDTH)) polyline.setConstantWidth(r.readBitDouble());
        if (has(flag, ELEVATION)) polyline.setElevation(r.readBitDouble());
        if (has(flag, THICKNESS)) polyline.setThickness(r.readBitDouble());
        if (has(flag, NORMAL)) polyline.setExtrusion(r.read3BitDouble());

        int pointCount = r.readBitLong();
        int bulgeCount = has(flag, BULGES) ? r.readBitLong() : 0;
        int vertexIdCount = v.from(DwgVersion.R2010) && has(flag, VERTEX_IDS) ? r.readBitLong() : 0;
        int widthCount = has(flag, WIDTHS) ? r.readBitLong() : 0;

        List<Point2D> points = new ArrayList<>();
        boolean relative = v.from(DwgVersion.R2000);
        for (int i = 0; i < pointCount; i++) {
            if (i > 0 && relative) {
                Point2D previous = points.get(i - 1);
                double[] p = r.read2DD(previous.x(), previous.y());
                points.add(new Point2D(p[0], p[1]));
            } else {
                double[] p = r.read2RawDouble();
                points.add(new Point2D(p[0], p[1]));
            }
        }
        polyline.setVertices(points);

        List<Double> bulges = new ArrayList<>();
        for (int i = 0; i < bulgeCount; i++) {
            bulges.add(r.readBitDouble());
        }
        polyline.setBulges(bulges);

        for (int i = 0; i < vertexIdCount; i++) {
            r.readBitLong();
        }

        List<double[]> widths = new ArrayList<>();
        for (int i = 0; i < widthCount; i++) {
            widths.add(new double[] {r.readBitDouble(), r.readBitDouble()});
        }
        polyline.setWidths(widths);
    }

    private static boolean has(int flag, int bit) {
        return (flag & bit) != 0;
    }
}
