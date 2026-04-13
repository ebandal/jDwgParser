package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLine;
import io.dwg.sections.objects.ObjectReader;

public class LineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLine line = (DwgLine) target;
        if (v.until(DwgVersion.R14)) {
            double[] start = r.read3RawDouble();
            double[] end   = r.read3RawDouble();
            line.setStart(new Point3D(start[0], start[1], start[2]));
            line.setEnd(new Point3D(end[0], end[1], end[2]));
        } else {
            boolean zAreZero = r.getInput().readBit();
            double sx = r.getInput().readRawDouble();
            double ex = r.getInput().readRawDouble();
            double sy = r.getInput().readRawDouble();
            double ey = r.getInput().readRawDouble();
            double sz = zAreZero ? 0.0 : r.getInput().readRawDouble();
            double ez = zAreZero ? 0.0 : r.getInput().readRawDouble();
            line.setStart(new Point3D(sx, sy, sz));
            line.setEnd(new Point3D(ex, ey, ez));
        }
        line.setThickness(r.readBitThickness());
        line.setExtrusion(r.readBitExtrusion());
    }
}
