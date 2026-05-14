package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLine;
import io.dwg.sections.objects.ObjectWriter;

public class LineObjectWriter implements ObjectWriter {

    @Override
    public int objectType() { return DwgObjectType.LINE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgLine line = (DwgLine) source;

        if (v.until(DwgVersion.R14)) {
            Point3D start = line.start();
            Point3D end = line.end();
            w.write3RawDouble(new double[]{start.x(), start.y(), start.z()});
            w.write3RawDouble(new double[]{end.x(), end.y(), end.z()});
        } else {
            Point3D start = line.start();
            Point3D end = line.end();
            boolean zAreZero = (start.z() == 0.0 && end.z() == 0.0);
            w.getOutput().writeBit(zAreZero);

            w.getOutput().writeRawDouble(start.x());
            w.getOutput().writeRawDouble(end.x());
            w.getOutput().writeRawDouble(start.y());
            w.getOutput().writeRawDouble(end.y());

            if (!zAreZero) {
                w.getOutput().writeRawDouble(start.z());
                w.getOutput().writeRawDouble(end.z());
            }
        }

        w.writeBitThickness(line.thickness());
        w.writeBitExtrusion(line.extrusion());
    }
}
