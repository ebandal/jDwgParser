package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLwPolyline;
import io.dwg.sections.objects.ObjectReader;
import java.util.ArrayList;
import java.util.List;

/**
 * LWPOLYLINE (경량 폴리라인) 객체 리더
 * 스펙 §20 LWPOLYLINE
 */
public class LwPolylineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LWPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLwPolyline lwp = (DwgLwPolyline) target;

        // 1. flags (BS - bit short, 2 bytes)
        int flags = r.readBitShort();
        lwp.setFlags(flags);

        // 2. constantWidth (BD - bit double)
        double constantWidth = r.readBitDouble();
        lwp.setConstantWidth(constantWidth);

        // 3. elevation (BD)
        double elevation = r.readBitDouble();
        lwp.setElevation(elevation);

        // 4. thickness (BT - bit thickness)
        double thickness = r.readBitThickness();
        lwp.setThickness(thickness);

        // 5. extrusion (BE - bit extrusion)
        double[] extrusion = r.readBitExtrusion();
        lwp.setExtrusion(extrusion);

        // 6. numVertices (BL - bit long, 4 bytes)
        long numVertices = r.readBitLong();

        // 7. bulgeCount (BL)
        long bulgeCount = r.readBitLong();

        // 8. widthCount (BL)
        long widthCount = r.readBitLong();

        // 9. numVerts (BL) - 반복 vertex count
        long numVerts = r.readBitLong();

        // 10. bulges - bulgeCount개
        List<Double> bulges = new ArrayList<>();
        for (int i = 0; i < bulgeCount; i++) {
            bulges.add(r.readBitDouble());
        }
        lwp.setBulges(bulges);

        // 11. widths - widthCount개 (각각 2개 double: start width, end width)
        List<double[]> widths = new ArrayList<>();
        for (int i = 0; i < widthCount; i++) {
            double startWidth = r.readBitDouble();
            double endWidth = r.readBitDouble();
            widths.add(new double[]{startWidth, endWidth});
        }
        lwp.setWidths(widths);

        // 12. vertices - numVerts개
        List<Point2D> vertices = new ArrayList<>();
        for (int i = 0; i < numVerts; i++) {
            double x = r.readBitDouble();
            double y = r.readBitDouble();
            vertices.add(new Point2D(x, y));
        }
        lwp.setVertices(vertices);
    }
}
