package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexMesh;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/** VERTEX (MESH) (type 12), spec §20.4.13: as VERTEX (3D), flags EC and point 3BD. */
public class VertexMeshObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_MESH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgVertexMesh vertex = (DwgVertexMesh) target;

        vertex.setFlags(r.getInput().readBits(8));          // flags (EC)
        double[] loc = r.read3BitDouble();                  // point (3BD)
        vertex.setLocation(Point3D.of(loc[0], loc[1], loc[2]));
    }
}
