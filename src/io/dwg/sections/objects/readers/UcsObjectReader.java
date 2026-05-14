package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgUcs;
import io.dwg.sections.objects.ObjectReader;

/**
 * UCS (사용자 좌표계) 테이블 엔트리 ObjectReader.
 * 타입 0x37
 */
public class UcsObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.UCS.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgUcs ucs = (DwgUcs) target;

        ucs.setName(r.readVariableText());

        double[] origin3d = r.read3BitDouble();
        ucs.setOrigin(new Point3D(origin3d[0], origin3d[1], origin3d[2]));

        double[] xDir3d = r.read3BitDouble();
        ucs.setXDirection(new Point3D(xDir3d[0], xDir3d[1], xDir3d[2]));

        double[] yDir3d = r.read3BitDouble();
        ucs.setYDirection(new Point3D(yDir3d[0], yDir3d[1], yDir3d[2]));
    }
}
