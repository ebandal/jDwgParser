package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgViewport;
import io.dwg.sections.objects.ObjectWriter;

public class ViewportObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.VIEWPORT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgViewport viewport = (DwgViewport) source;
        Point3D center = viewport.center();
        w.write3BitDouble(new double[]{center.x(), center.y(), center.z()});
        w.getOutput().writeRawDouble(viewport.width());
        w.getOutput().writeRawDouble(viewport.height());
        w.getOutput().writeRawDouble(viewport.viewHeight());
        w.writeBitExtrusion(viewport.extrusion());
    }
}
