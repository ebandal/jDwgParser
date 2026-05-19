package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * TEXT 엔티티 리더 (타입 0x01)
 * libredwg dwg.spec DWG_ENTITY(TEXT)
 */
public class TextObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.TEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgText text = (DwgText) target;

        if (v.until(DwgVersion.R13)) {
            // R13 이전: elevation(BD) + ins_pt(2RD)
            text.setElevation(r.readBitDouble());
            double[] pt = r.read2RawDouble();
            text.setInsertionPoint(new Point2D(pt[0], pt[1]));
        } else if (v.until(DwgVersion.R14)) {
            // R13/R14: elevation(BD), ins_pt(2RD), alignment_pt(2RD),
            //          extrusion(3BD), thickness(BD), oblique(BD), rotation(BD),
            //          height(BD), widthFactor(BD), value(T), generation(BS),
            //          horiz(BS), vert(BS)
            text.setElevation(r.readBitDouble());
            double[] pt = r.read2RawDouble();
            text.setInsertionPoint(new Point2D(pt[0], pt[1]));
            double[] apt = r.read2RawDouble();
            text.setAlignmentPoint(new Point2D(apt[0], apt[1]));
            r.read3BitDouble(); // extrusion (3BD)
            text.setThickness(r.readBitDouble());
            text.setObliquAngle(r.readBitDouble());
            text.setRotationAngle(r.readBitDouble());
            text.setHeight(r.readBitDouble());
            text.setWidthFactor(r.readBitDouble());
            text.setValue(r.readVariableText());
            text.setGeneration(r.readBitShort());
            text.setHorizontalAlignment(r.readBitShort());
            text.setVerticalAlignment(r.readBitShort());
        } else {
            // R2000+: dataflags-driven fields using RD (not BD) for most doubles
            int dataFlags = r.getInput().readRawChar();
            text.setDataFlags(dataFlags);

            if ((dataFlags & 0x01) == 0) text.setElevation(r.readRawDouble());
            double[] pt = r.read2RawDouble();
            text.setInsertionPoint(new Point2D(pt[0], pt[1]));
            if ((dataFlags & 0x02) == 0) {
                double[] dd = r.read2DD(pt[0], pt[1]);
                text.setAlignmentPoint(new Point2D(dd[0], dd[1]));
            }
            text.setExtrusion(r.readBitExtrusion());
            text.setThickness(r.readBitThickness());
            if ((dataFlags & 0x04) == 0) text.setObliquAngle(r.readRawDouble());
            if ((dataFlags & 0x08) == 0) text.setRotationAngle(r.readRawDouble());
            text.setHeight(r.readRawDouble());
            if ((dataFlags & 0x10) == 0) {
                text.setWidthFactor(r.readRawDouble());
            } else {
                text.setWidthFactor(1.0);
            }
            text.setValue(r.readVariableText());
            if ((dataFlags & 0x20) == 0) text.setGeneration(r.readBitShort());
            if ((dataFlags & 0x40) == 0) text.setHorizontalAlignment(r.readBitShort());
            if ((dataFlags & 0x80) == 0) text.setVerticalAlignment(r.readBitShort());
        }

        text.setStyleHandle(new DwgHandleRef(r.readHandle()));
    }
}
