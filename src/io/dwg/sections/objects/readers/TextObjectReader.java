package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.ObjectReader;

public class TextObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.TEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgText text = (DwgText) target;

        if (v.until(DwgVersion.R13)) {
            text.setElevation(r.readBitDouble());
            double[] pt = r.read2RawDouble();
            text.setInsertionPoint(new Point2D(pt[0], pt[1]));
        } else {
            int dataFlags = r.getInput().readRawChar();
            text.setDataFlags(dataFlags);
            if ((dataFlags & 0x01) == 0) text.setElevation(r.readBitDouble());
            double[] pt = r.read2RawDouble();
            text.setInsertionPoint(new Point2D(pt[0], pt[1]));
            if ((dataFlags & 0x02) == 0) {
                double[] apt = r.read2RawDouble();
                text.setAlignmentPoint(new Point2D(apt[0], apt[1]));
            }
            text.setExtrusion(r.readBitExtrusion());
            text.setThickness(r.readBitThickness());
            if ((dataFlags & 0x04) == 0) text.setObliquAngle(r.readBitDouble());
            if ((dataFlags & 0x08) == 0) text.setRotationAngle(r.readBitDouble());
            text.setHeight(r.readBitDouble());
            if ((dataFlags & 0x10) == 0) text.setWidthFactor(r.readBitDouble());
            text.setValue(r.readVariableText());
            if ((dataFlags & 0x20) == 0) text.setGeneration(r.readBitShort());
            if ((dataFlags & 0x40) == 0) text.setHorizontalAlignment(r.readBitShort());
            if ((dataFlags & 0x80) == 0) text.setVerticalAlignment(r.readBitShort());
        }

        // handle refs: style
        text.setStyleHandle(new DwgHandleRef(r.readHandle()));
    }
}
