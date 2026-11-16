package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertex3D;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_3D 엔티티 리더 (타입 0x0B)
 * 3D 폴리라인의 꼭지점
 * 스펙 §20 VERTEX_3D
 */
public class Vertex3DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgVertex3D vertex = (DwgVertex3D) target;
        vertex.setFlags(r.getInput().readBits(8));          // flags (EC)
        double[] loc = r.read3BitDouble();                  // point (3BD), WCS
        vertex.setLocation(new Point3D(loc[0], loc[1], loc[2]));
    }
}
