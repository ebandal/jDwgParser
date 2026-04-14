package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLine;
import io.dwg.sections.objects.ObjectWriter;

public class MLineObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.MLINE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgMLine mline = (DwgMLine) source;
        w.writeBitExtrusion(mline.extrusion());
        w.writeVariableText(mline.styleHandle());
        w.getOutput().writeRawShort((short) mline.justification());
        w.getOutput().writeRawDouble(mline.scale());
        w.getOutput().writeRawShort((short) mline.vertices().size());
        for (Point3D vertex : mline.vertices()) {
            w.write3BitDouble(new double[]{vertex.x(), vertex.y(), vertex.z()});
        }
    }
}
