package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTRIB (type 2), spec §20.4.4: the TEXT fields of §20.4.3, then the attribute fields.
 */
public class AttribObjectReader implements ObjectReader {

    /** The attribute fields shared by ATTRIB and ATTDEF. */
    static final class AttributeFields {
        String tag;
        int flags;
    }

    @Override
    public int objectType() { return DwgObjectType.ATTRIB.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgAttrib attrib = (DwgAttrib) target;
        DwgText text = attrib.textData();
        TextObjectReader.readTextData(text, r, v);
        if (text.insertionPoint() != null) {
            attrib.setLocation(Point3D.of(text.insertionPoint().x(), text.insertionPoint().y(), text.elevation()));
        }
        attrib.setText(text.value());
        attrib.setHeight(text.height());
        attrib.setAngle(text.rotationAngle());

        AttributeFields fields = readAttributeFields(r, v);
        attrib.setTag(fields.tag);
        attrib.setFlags(fields.flags);
    }

    /**
     * The attribute fields of §20.4.4 that follow the TEXT fields:
     * <pre>
     *   R2010+  version (RC)
     *   R2018+  attribute type (RC): 1 single line, 2 multi-line ATTRIB, 4 multi-line ATTDEF;
     *           for multi-line ones the embedded MTEXT and the annotative data follow
     *           tag (TV), field length (BS), flags (RC)
     *   R2007+  lock position (B)
     * </pre>
     */
    static AttributeFields readAttributeFields(BitStreamReader r, DwgVersion v) throws Exception {
        if (v.from(DwgVersion.R2010)) {
            r.getInput().readBits(8);                    // version
        }
        if (v.from(DwgVersion.R2018)) {
            int type = r.getInput().readBits(8);
            if (type > 1) {
                EntityHeaderReader.skipMTextEmbedded(r, v);
            }
        }
        AttributeFields fields = new AttributeFields();
        fields.tag = r.readVariableText();
        r.readBitShort();                                // field length
        fields.flags = r.getInput().readBits(8);
        if (v.from(DwgVersion.R2007)) {
            r.getInput().readBit();                      // lock position
        }
        return fields;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgAttrib) target).textData().setStyleHandle(r.readRef(target.handle()));
    }
}
