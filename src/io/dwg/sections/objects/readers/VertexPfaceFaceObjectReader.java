package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexPfaceFace;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_PFACE_FACE 엔티티 리더 (타입 0x0E)
 * 면 폴리라인의 면 정의
 */
public class VertexPfaceFaceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_PFACE_FACE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVertexPfaceFace vertex = (DwgVertexPfaceFace) target;

        // 1. vertex1Index (BS)
        int v1 = r.readBitShort();
        vertex.setVertex1Index(v1);

        // 2. vertex2Index (BS)
        int v2 = r.readBitShort();
        vertex.setVertex2Index(v2);

        // 3. vertex3Index (BS)
        int v3 = r.readBitShort();
        vertex.setVertex3Index(v3);

        // 4. vertex4Index (BS)
        int v4 = r.readBitShort();
        vertex.setVertex4Index(v4);
    }
}
