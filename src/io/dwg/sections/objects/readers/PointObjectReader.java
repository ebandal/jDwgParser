package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPoint;
import io.dwg.sections.objects.ObjectReader;

/**
 * POINT 엔티티 리더
 * 스펙 §20 POINT
 */
public class PointObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POINT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPoint point = (DwgPoint) target;

        // 1. position (3BD)
        double[] posData = r.read3BitDouble();
        Point3D position = new Point3D(posData[0], posData[1], posData[2]);
        point.setPosition(position);

        // 2. thickness (BT)
        double thickness = r.readBitThickness();
        point.setThickness(thickness);

        // 3. extrusion (BE)
        double[] extrusion = r.readBitExtrusion();
        point.setExtrusion(extrusion);

        // 4. xAxisAngle (BD)
        double xAxisAngle = r.readBitDouble();
        point.setXAxisAngle(xAxisAngle);
    }
}
