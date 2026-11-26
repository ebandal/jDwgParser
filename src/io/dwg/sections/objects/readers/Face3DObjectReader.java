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
 * 3DFACE (type 28), spec §20.4.32.
 * <pre>
 *   R13–R14  four corners 3BD, invisible edge flags BS
 *   R2000+   "has no flag ind." B, "Z is zero" B,
 *            first corner x RD, y RD, z RD (only if Z is not zero),
 *            corners 2–4 as 3DD with the previous corner as default,
 *            invisible edge flags BS (only if "has no flag ind." is 0)
 * </pre>
 */
public class Face3DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.FACE3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgFace3D face = (DwgFace3D) target;
        Point3D[] corners = new Point3D[4];

        if (!v.from(DwgVersion.R2000)) {
            for (int i = 0; i < corners.length; i++) {
                double[] p = r.read3BitDouble();
                corners[i] = Point3D.of(p[0], p[1], p[2]);
            }
            face.setInvisibleEdges(r.readBitShort());
        } else {
            boolean noFlags = r.getInput().readBit();
            boolean zIsZero = r.getInput().readBit();
            double x = r.readRawDouble();
            double y = r.readRawDouble();
            double z = zIsZero ? 0.0 : r.readRawDouble();
            corners[0] = Point3D.of(x, y, z);
            for (int i = 1; i < corners.length; i++) {
                Point3D previous = corners[i - 1];
                double[] p = r.read3DD(previous.x(), previous.y(), previous.z());
                corners[i] = Point3D.of(p[0], p[1], p[2]);
            }
            if (!noFlags) {
                face.setInvisibleEdges(r.readBitShort());
            }
        }
        face.setPoints(corners);
    }
}
