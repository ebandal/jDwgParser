package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLine;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * MLINE (type 47), spec §20.4.50: scale BD, justification EC, base point 3BD, extrusion 3BD,
 * open/closed BS, lines in style RC, number of vertices BS; each vertex: point, direction and
 * miter direction 3BD, then for each line: number of segment parameters BS and the BDs, number
 * of area fill parameters BS and the BDs. Handle: the MLINESTYLE.
 */
public class MLineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgMLine mline = (DwgMLine) target;
        mline.setScale(r.readBitDouble());
        mline.setJustification(r.readRawChar());
        Fields.read(mline, r, "BASEPOINT 3BD");
        mline.setExtrusion(r.read3BitDouble());
        mline.setOpenClosed(r.readBitShort());
        int lines = r.readRawChar();
        int count = r.readBitShort();
        for (int i = 0; i < count; i++) {
            Point3D point = point(r);
            Point3D direction = point(r);
            Point3D miter = point(r);
            java.util.List<double[]> segments = new java.util.ArrayList<>();
            java.util.List<double[]> fills = new java.util.ArrayList<>();
            for (int j = 0; j < lines; j++) {
                segments.add(doubles(r, r.readBitShort()));
                fills.add(doubles(r, r.readBitShort()));
            }
            mline.addVertex(point);
            mline.addVertexData(new DwgMLine.Vertex(point, direction, miter, segments, fills));
        }
    }

    private static Point3D point(BitStreamReader r) {
        double[] p = r.read3BitDouble();
        return new Point3D(p[0], p[1], p[2]);
    }

    private static double[] doubles(BitStreamReader r, int n) {
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            d[i] = r.readBitDouble();
        }
        return d;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgMLine) target).setMlineStyleHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
