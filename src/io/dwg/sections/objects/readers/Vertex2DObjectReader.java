package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertex2D;
import io.dwg.sections.objects.EntityHeaderReader;
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
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgVertex2D vertex = (DwgVertex2D) target;
        vertex.setFlags(r.getInput().readBits(8));          // flags (EC)
        double[] loc = r.read3BitDouble();                  // point (3BD), OCS
        vertex.setLocation(Point3D.of(loc[0], loc[1], loc[2]));
        // a negative start width stands for both widths, and no end width follows
        double startWidth = r.readBitDouble();
        if (startWidth < 0) {
            vertex.setStartWidth(-startWidth);
            vertex.setEndWidth(-startWidth);
        } else {
            vertex.setStartWidth(startWidth);
            vertex.setEndWidth(r.readBitDouble());
        }
        vertex.setBulge(r.readBitDouble());
        if (v.from(DwgVersion.R2010)) {
            r.readBitLong();                                // vertex id
        }
        vertex.setTangentDirection(r.readBitDouble());
    }
}
