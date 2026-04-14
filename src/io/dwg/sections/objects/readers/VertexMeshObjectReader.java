package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexMesh;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_MESH 엔티티 리더 (타입 0x0C)
 * 메시 폴리라인의 꼭지점
 */
public class VertexMeshObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_MESH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVertexMesh vertex = (DwgVertexMesh) target;

        // 1. location (3RD)
        double[] loc = r.read3RawDouble();
        vertex.setLocation(new Point3D(loc[0], loc[1], loc[2]));

        // 2. flags (BS)
        int flags = r.readBitShort();
        vertex.setFlags(flags);
    }
}
