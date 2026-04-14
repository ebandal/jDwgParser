package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSpline;
import io.dwg.sections.objects.ObjectWriter;
import java.util.List;

public class SplineObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.SPLINE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgSpline spline = (DwgSpline) source;
        w.writeBitShort(spline.degree());

        List<Point3D> fitPoints = spline.fitPoints();
        w.writeBitLong(fitPoints.size());
        for (Point3D pt : fitPoints) {
            w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        }

        w.writeBitShort(spline.flags());

        List<Point3D> controlPoints = spline.controlPoints();
        w.writeBitLong(controlPoints.size());
        for (Point3D pt : controlPoints) {
            w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        }

        w.writeBitExtrusion(spline.extrusion());
    }
}
