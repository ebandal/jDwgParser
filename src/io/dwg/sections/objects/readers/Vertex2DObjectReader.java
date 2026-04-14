package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertex2D;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_2D 엔티티 리더 (타입 0x0A)
 * 2D 폴리라인의 꼭지점
 * 스펙 §20 VERTEX_2D
 */
public class Vertex2DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_2D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVertex2D vertex = (DwgVertex2D) target;

        // 1. location (3RD - 3 raw doubles)
        double[] loc = r.read3RawDouble();
        vertex.setLocation(new Point3D(loc[0], loc[1], loc[2]));

        // 2. startWidth (BD - bit double)
        double startWidth = r.readBitDouble();
        vertex.setStartWidth(startWidth);

        // 3. endWidth (BD)
        double endWidth = r.readBitDouble();
        vertex.setEndWidth(endWidth);

        // 4. bulge (BD)
        double bulge = r.readBitDouble();
        vertex.setBulge(bulge);

        // 5. flags (BS - bit short)
        int flags = r.readBitShort();
        vertex.setFlags(flags);
    }
}
