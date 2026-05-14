package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.ObjectWriter;

public class TextObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.TEXT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgText text = (DwgText) source;
        Point2D pt = text.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), text.elevation()});
        w.writeBitDouble(text.height());
        w.writeVariableText(text.value());
        w.writeBitDouble(text.rotationAngle());
        w.writeBitDouble(text.widthFactor());
        w.writeBitDouble(text.obliquAngle());
        w.writeBitShort(text.horizontalAlignment());
        w.writeBitExtrusion(text.extrusion());
        w.writeBitThickness(text.thickness());
    }
}
