package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * MTEXT 엔티티 리더 (타입 0x2C)
 * libredwg dwg.spec DWG_ENTITY(MTEXT)
 */
public class MTextObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MTEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgMText mtext = (DwgMText) target;

        // ins_pt (3BD)
        double[] loc = r.read3BitDouble();
        mtext.setLocation(new Point3D(loc[0], loc[1], loc[2]));

        // extrusion (3BD — not BE)
        double[] ext = r.read3BitDouble();
        mtext.setExtrusion(ext);

        // x_axis_dir (3BD)
        r.read3BitDouble();

        // rect_width (BD), [R2007+: rect_height (BD)], text_height (BD)
        double rectWidth = r.readBitDouble();
        mtext.setWidth(rectWidth);
        if (v.from(DwgVersion.R2007)) {
            r.readBitDouble(); // rect_height
        }
        double textHeight = r.readBitDouble();
        mtext.setHeight(textHeight);

        // attachment (BS), flow_dir (BS)
        int attachment = r.readBitShort();
        mtext.setAttachmentPoint(attachment);
        r.readBitShort(); // flow_dir

        // extents_height (BD), extents_width (BD)
        r.readBitDouble();
        r.readBitDouble();

        // text (T)
        String text = r.readVariableText();
        mtext.setText(text);

        // style handle
        r.readHandle();

        // R2000+: linespace_style (BS), linespace_factor (BD), unknown_b0 (B)
        if (v.from(DwgVersion.R2000)) {
            r.readBitShort();
            r.readBitDouble();
            r.getInput().readBit();
        }
    }
}
