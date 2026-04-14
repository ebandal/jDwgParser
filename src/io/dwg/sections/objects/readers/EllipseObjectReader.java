package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgEllipse;
import io.dwg.sections.objects.ObjectReader;

/**
 * ELLIPSE 엔티티 리더
 * 스펙 §20 ELLIPSE
 */
public class EllipseObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ELLIPSE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgEllipse ellipse = (DwgEllipse) target;

        // 1. center point (3BD)
        double[] centerData = r.read3BitDouble();
        Point3D center = new Point3D(centerData[0], centerData[1], centerData[2]);
        ellipse.setCenter(center);

        // 2. majorAxisVec (3BD)
        double[] majorData = r.read3BitDouble();
        Point3D majorAxisVec = new Point3D(majorData[0], majorData[1], majorData[2]);
        ellipse.setMajorAxisVec(majorAxisVec);

        // 3. extrusion (BE - bit extrusion)
        double[] extrusion = r.readBitExtrusion();
        ellipse.setExtrusion(extrusion);

        // 4. axisRatio (BD)
        double axisRatio = r.readBitDouble();
        ellipse.setAxisRatio(axisRatio);

        // 5. startParam (BD)
        double startParam = r.readBitDouble();
        ellipse.setStartParam(startParam);

        // 6. endParam (BD)
        double endParam = r.readBitDouble();
        ellipse.setEndParam(endParam);
    }
}
