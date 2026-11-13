package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLine;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

public class LineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgLine line = (DwgLine) target;
        if (v.until(DwgVersion.R14)) {
            double[] start = r.read3RawDouble();
            double[] end   = r.read3RawDouble();
            line.setStart(new Point3D(start[0], start[1], start[2]));
            line.setEnd(new Point3D(end[0], end[1], end[2]));
        } else {
            boolean zAreZero = r.getInput().readBit();
            // 끝점은 시작점을 기본값으로 하는 DD로 저장된다
            double sx = r.readRawDouble();
            double ex = r.readDD(sx);
            double sy = r.readRawDouble();
            double ey = r.readDD(sy);
            double sz = zAreZero ? 0.0 : r.readRawDouble();
            double ez = zAreZero ? 0.0 : r.readDD(sz);
            line.setStart(new Point3D(sx, sy, sz));
            line.setEnd(new Point3D(ex, ey, ez));
        }
        line.setThickness(r.readBitThickness());
        line.setExtrusion(r.readBitExtrusion());
    }
}
