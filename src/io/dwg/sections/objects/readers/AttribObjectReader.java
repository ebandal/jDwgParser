package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.concrete.DwgText;

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

        // AcDbText subclass : the same layout as TEXT
        DwgText text = attrib.textData();
        TextObjectReader.readTextData(text, r, v);
        if (text.insertionPoint() != null) {
            attrib.setLocation(new Point3D(text.insertionPoint().x(), text.insertionPoint().y(), text.elevation()));
        }
        attrib.setText(text.value());
        attrib.setHeight(text.height());
        attrib.setAngle(text.rotationAngle());

        // AcDbAttribute subclass
        if (v.from(DwgVersion.R2010)) r.getInput().readBits(8); // class version
        if (v.from(DwgVersion.R2018)) {
            int mtextType = r.getInput().readBits(8); // mtext_type
            if (mtextType > 1) {
                EntityHeaderReader.skipMTextEmbedded(r, v);
            }
        }
        attrib.setTag(r.readVariableText());
        r.readBitShort();                               // field_length
        attrib.setFlags(r.getInput().readBits(8));      // flags
        if (v.from(DwgVersion.R2007)) r.getInput().readBit(); // lock_position_flag
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgAttrib) target).textData().setStyleHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
