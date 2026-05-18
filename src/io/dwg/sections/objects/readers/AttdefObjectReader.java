package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttdef;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTDEF 엔티티 리더 (타입 0x02)
 * libredwg dwg.spec DWG_ENTITY(ATTDEF)
 *
 * AcDbText 서브클래스 본문은 TEXT와 동일.
 * 그 뒤에 AcDbAttributeDefinition 서브클래스(tag, prompt 등)가 온다.
 */
public class AttdefObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ATTDEF.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgAttdef attdef = (DwgAttdef) target;

        if (v.until(DwgVersion.R13)) {
            // pre-R13: ins_pt(2RD), height(RD), default_value(TV), ...
            double[] pt = r.read2RawDouble();
            attdef.setInsertionPoint(new Point3D(pt[0], pt[1], 0.0));
            r.readRawDouble(); // height
            attdef.setDefaultValue(r.readVariableText());
            attdef.setPrompt(r.readVariableText());
            attdef.setTag(r.readVariableText());
        } else if (v.until(DwgVersion.R14)) {
            // R13/R14: elevation(BD), ins_pt(2RD), alignment_pt(2RD),
            //          extrusion(3BD), thickness(BD), oblique(BD), rotation(BD),
            //          height(BD), widthFactor(BD), default_value(T),
            //          generation(BS), horiz(BS), vert(BS)
            //          then ATTDEF: tag(T), field_length(BS), flags(RC), prompt(T)
            r.readBitDouble(); // elevation
            double[] pt = r.read2RawDouble();
            attdef.setInsertionPoint(new Point3D(pt[0], pt[1], 0.0));
            r.read2RawDouble(); // alignment_pt
            r.read3BitDouble(); // extrusion
            r.readBitDouble();  // thickness
            r.readBitDouble();  // oblique
            r.readBitDouble();  // rotation
            r.readBitDouble();  // height
            r.readBitDouble();  // widthFactor
            attdef.setDefaultValue(r.readVariableText());
            r.readBitShort();   // generation
            r.readBitShort();   // horiz_alignment
            r.readBitShort();   // vert_alignment

            attdef.setTag(r.readVariableText());
            r.readBitShort();   // field_length
            attdef.setFlags(r.getInput().readRawChar());
            attdef.setPrompt(r.readVariableText());
        } else {
            // R2000+: dataflags-driven (same as TEXT R2000+ body) then ATTDEF subclass
            int dataFlags = r.getInput().readRawChar();

            if ((dataFlags & 0x01) == 0) r.readRawDouble(); // elevation
            double[] pt = r.read2RawDouble();
            attdef.setInsertionPoint(new Point3D(pt[0], pt[1], 0.0));
            if ((dataFlags & 0x02) == 0) r.read2DD(pt[0], pt[1]); // alignment_pt
            r.readBitExtrusion();
            r.readBitThickness();
            if ((dataFlags & 0x04) == 0) r.readRawDouble(); // oblique
            if ((dataFlags & 0x08) == 0) r.readRawDouble(); // rotation
            r.readRawDouble(); // height
            if ((dataFlags & 0x10) == 0) r.readRawDouble(); // widthFactor
            attdef.setDefaultValue(r.readVariableText());
            if ((dataFlags & 0x20) == 0) r.readBitShort(); // generation
            if ((dataFlags & 0x40) == 0) r.readBitShort(); // horiz_alignment
            if ((dataFlags & 0x80) == 0) r.readBitShort(); // vert_alignment

            // AcDbAttributeDefinition subclass
            if (v.from(DwgVersion.R2010)) r.getInput().readRawChar(); // is_locked_in_block
            if (v.from(DwgVersion.R2018)) r.getInput().readRawChar(); // mtext_type (skip embedded subclass)
            attdef.setTag(r.readVariableText());
            r.readBitShort();   // field_length
            attdef.setFlags(r.getInput().readRawChar());
            if (v.from(DwgVersion.R2007)) r.getInput().readBit(); // lock_position_flag
            if (v.from(DwgVersion.R2010)) r.getInput().readRawChar(); // keep_duplicate_records
            attdef.setPrompt(r.readVariableText());
        }

        // style handle
        r.readHandle();
    }
}
