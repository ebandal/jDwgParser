package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolylineMesh;
import io.dwg.sections.objects.ObjectReader;

/**
 * POLYLINE_MESH 엔티티 리더 (타입 0x1E)
 * 메시 폴리라인 (M과 N 방향의 격자)
 */
public class PolylineMeshObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POLYLINE_MESH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPolylineMesh polyline = (DwgPolylineMesh) target;

        // 1. flags (BS)
        int flags = r.readBitShort();
        polyline.setFlags(flags);

        // 2. mVertexCount (BS)
        int mCount = r.readBitShort();
        polyline.setMVertexCount(mCount);

        // 3. nVertexCount (BS)
        int nCount = r.readBitShort();
        polyline.setNVertexCount(nCount);

        // 4. mDensity (BS)
        int mDensity = r.readBitShort();
        polyline.setMDensity(mDensity);

        // 5. nDensity (BS)
        int nDensity = r.readBitShort();
        polyline.setNDensity(nDensity);

        // 6. extrusion (BE)
        double[] extrusion = r.readBitExtrusion();
        polyline.setExtrusion(extrusion);
    }
}
