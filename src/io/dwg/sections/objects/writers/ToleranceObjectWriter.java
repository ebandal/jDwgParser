package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTolerance;
import io.dwg.sections.objects.ObjectWriter;

public class ToleranceObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.TOLERANCE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgTolerance tolerance = (DwgTolerance) source;
        Point3D pt = tolerance.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.writeVariableText(tolerance.dimensionStyleName());
        w.writeVariableText(tolerance.toleranceText());
        double[] dir = tolerance.direction();
        w.write3RawDouble(dir);
    }
}
