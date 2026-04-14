package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexPface;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_PFACE 엔티티 리더 (타입 0x0D)
 * 면 폴리라인의 꼭지점
 */
public class VertexPfaceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_PFACE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVertexPface vertex = (DwgVertexPface) target;

        // 1. location (3RD)
        double[] loc = r.read3RawDouble();
        vertex.setLocation(new Point3D(loc[0], loc[1], loc[2]));

        // 2. flags (BS)
        int flags = r.readBitShort();
        vertex.setFlags(flags);
    }
}
