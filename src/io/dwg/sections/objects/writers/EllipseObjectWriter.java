package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgEllipse;
import io.dwg.sections.objects.ObjectWriter;

public class EllipseObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.ELLIPSE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgEllipse ellipse = (DwgEllipse) source;
        Point3D center = ellipse.center();
        w.write3RawDouble(new double[]{center.x(), center.y(), center.z()});
        Point3D majAxis = ellipse.majorAxisVec();
        w.write3RawDouble(new double[]{majAxis.x(), majAxis.y(), majAxis.z()});
        w.writeBitDouble(ellipse.axisRatio());
        w.writeBitDouble(ellipse.startParam());
        w.writeBitDouble(ellipse.endParam());
        w.writeBitExtrusion(ellipse.extrusion());
    }
}
