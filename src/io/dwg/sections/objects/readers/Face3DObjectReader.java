package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgFace3D;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * 3DFACE 엔티티 리더 (타입 0x1C)
 * 스펙 §20 / libredwg dwg.spec DWG_ENTITY(_3DFACE)
 */
public class Face3DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.FACE3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgFace3D face = (DwgFace3D) target;
        Point3D[] points = new Point3D[4];

        if (v.until(DwgVersion.R14)) {
            // R13/R14: 4 × 3BD
            for (int i = 0; i < 4; i++) {
                double[] p = r.read3BitDouble();
                points[i] = new Point3D(p[0], p[1], p[2]);
            }
        } else {
            // R2000+: has_no_flags(B), z_is_zero(B), corner1(RD,RD,[RD]),
            //         corner2/3/4 as 3DD relative to previous
            boolean hasNoFlags = r.getInput().readBit();
            boolean zIsZero    = r.getInput().readBit();

            double x1 = r.readRawDouble();
            double y1 = r.readRawDouble();
            double z1 = zIsZero ? 0.0 : r.readRawDouble();
            points[0] = new Point3D(x1, y1, z1);

            double[] p2 = r.read3DD(x1, y1, z1);
            points[1] = new Point3D(p2[0], p2[1], p2[2]);

            double[] p3 = r.read3DD(p2[0], p2[1], p2[2]);
            points[2] = new Point3D(p3[0], p3[1], p3[2]);

            double[] p4 = r.read3DD(p3[0], p3[1], p3[2]);
            points[3] = new Point3D(p4[0], p4[1], p4[2]);

            if (!hasNoFlags) {
                r.readBitShort(); // invis_flags (BS)
            }
        }

        face.setPoints(points);
    }
}
