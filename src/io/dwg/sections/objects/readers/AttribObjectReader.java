package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTRIB 엔티티 리더 (타입 0x03)
 * libredwg dwg.spec DWG_ENTITY(ATTRIB)
 *
 * AcDbText 서브클래스 본문은 ATTDEF와 동일 (default_value 대신 text_value).
 * 그 뒤에 AcDbAttribute 서브클래스(tag 등)가 온다. prompt 없음.
 */
public class AttribObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ATTRIB.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgAttrib attrib = (DwgAttrib) target;

        if (v.until(DwgVersion.R13)) {
            double[] pt = r.read2RawDouble();
            attrib.setLocation(new Point3D(pt[0], pt[1], 0.0));
            r.readRawDouble(); // height
            attrib.setText(r.readVariableText());
            attrib.setTag(r.readVariableText());
            r.getInput().readRawChar(); // flags
        } else if (v.until(DwgVersion.R14)) {
            r.readBitDouble(); // elevation
            double[] pt = r.read2RawDouble();
            attrib.setLocation(new Point3D(pt[0], pt[1], 0.0));
            r.read2RawDouble(); // alignment_pt
            r.read3BitDouble(); // extrusion
            r.readBitDouble();  // thickness
            r.readBitDouble();  // oblique_angle
            r.readBitDouble();  // rotation
            r.readBitDouble();  // height
            r.readBitDouble();  // width_factor
            attrib.setText(r.readVariableText());
            r.readBitShort();   // generation
            r.readBitShort();   // horiz_alignment
            r.readBitShort();   // vert_alignment
        } else {
            // R2000+: dataflags-driven (same layout as ATTDEF AcDbText subclass)
            int dataFlags = r.getInput().readRawChar();

            if ((dataFlags & 0x01) == 0) r.readRawDouble(); // elevation
            double[] pt = r.read2RawDouble();
            attrib.setLocation(new Point3D(pt[0], pt[1], 0.0));
            if ((dataFlags & 0x02) == 0) r.read2DD(pt[0], pt[1]); // alignment_pt
            r.readBitExtrusion();
            r.readBitThickness();
            if ((dataFlags & 0x04) == 0) r.readRawDouble(); // oblique_angle
            if ((dataFlags & 0x08) == 0) r.readRawDouble(); // rotation
            r.readRawDouble(); // height (always)
            if ((dataFlags & 0x10) == 0) r.readRawDouble(); // width_factor
            attrib.setText(r.readVariableText());
            if ((dataFlags & 0x20) == 0) r.readBitShort(); // generation
            if ((dataFlags & 0x40) == 0) r.readBitShort(); // horiz_alignment
            if ((dataFlags & 0x80) == 0) r.readBitShort(); // vert_alignment
        }

        // AcDbAttribute subclass
        if (v.from(DwgVersion.R2010)) r.getInput().readRawChar(); // is_locked_in_block
        if (v.from(DwgVersion.R2018)) r.getInput().readRawChar(); // mtext_type
        if (v.from(DwgVersion.R13)) {
            attrib.setTag(r.readVariableText());
            r.readBitShort();   // field_length
            r.getInput().readRawChar(); // flags
            if (v.from(DwgVersion.R2007)) r.getInput().readBit(); // lock_position_flag
            if (v.from(DwgVersion.R2010)) r.getInput().readRawChar(); // keep_duplicate_records
        }

        r.readHandle(); // style handle
    }
}
