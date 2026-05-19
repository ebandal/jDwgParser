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
 * LWPOLYLINE 엔티티 리더 (타입 0x4B)
 * libredwg dwg.spec DWG_ENTITY(LWPOLYLINE)
 *
 * flag bits:
 *   1=extrusion, 2=thickness, 4=const_width, 8=elevation,
 *   16=num_bulges, 32=num_widths, 512=closed, 1024=vertexids (R2010+)
 */
public class LwPolylineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LWPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgLwPolyline lwp = (DwgLwPolyline) target;

        int flags = r.readBitShort();
        lwp.setFlags(flags);

        if ((flags & 4) != 0)  lwp.setConstantWidth(r.readBitDouble());
        if ((flags & 8) != 0)  lwp.setElevation(r.readBitDouble());
        if ((flags & 2) != 0)  lwp.setThickness(r.readBitDouble());
        if ((flags & 1) != 0) {
            double[] ext = r.read3BitDouble();
            lwp.setExtrusion(ext);
        }

        int numPoints = r.readBitLong();

        int numBulges = 0;
        if ((flags & 16) != 0) numBulges = r.readBitLong();

        if (v.from(DwgVersion.R2010) && (flags & 1024) != 0) {
            r.readBitLong(); // num_vertexids — skip
        }

        int numWidths = 0;
        if ((flags & 32) != 0) numWidths = r.readBitLong();

        // points: first as 2RD, subsequent as 2DD relative to previous
        List<Point2D> vertices = new ArrayList<>();
        double prevX = 0, prevY = 0;
        for (int i = 0; i < numPoints; i++) {
            double x, y;
            if (i == 0 || v.until(DwgVersion.R14)) {
                x = r.readRawDouble();
                y = r.readRawDouble();
            } else {
                double[] dd = r.read2DD(prevX, prevY);
                x = dd[0]; y = dd[1];
            }
            vertices.add(new Point2D(x, y));
            prevX = x; prevY = y;
        }
        lwp.setVertices(vertices);

        // bulges
        List<Double> bulges = new ArrayList<>();
        for (int i = 0; i < numBulges; i++) bulges.add(r.readBitDouble());
        lwp.setBulges(bulges);

        // vertexids (R2010+): already counted, skip individual reads done above

        // widths
        List<double[]> widths = new ArrayList<>();
        for (int i = 0; i < numWidths; i++) {
            widths.add(new double[]{r.readBitDouble(), r.readBitDouble()});
        }
        lwp.setWidths(widths);
    }
}
