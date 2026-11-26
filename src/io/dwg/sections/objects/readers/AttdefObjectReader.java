package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttdef;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTDEF (type 3), spec §20.4.5: the ATTRIB data of §20.4.4 (TEXT fields and attribute fields),
 * then for R2010+ a version (RC), then the prompt (TV).
 */
public class AttdefObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ATTDEF.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgAttdef attdef = (DwgAttdef) target;
        DwgText text = new DwgText();
        TextObjectReader.readTextData(text, r, v);
        attdef.setInsertionPoint(Point3D.of(text.insertionPoint().x(), text.insertionPoint().y(), 0.0));
        attdef.setDefaultValue(text.value());

        AttribObjectReader.AttributeFields fields = AttribObjectReader.readAttributeFields(r, v);
        attdef.setTag(fields.tag);
        attdef.setFlags(fields.flags);
        if (v.from(DwgVersion.R2010)) {
            r.getInput().readBits(8);                    // ATTDEF version
        }
        attdef.setPrompt(r.readVariableText());
    }

    /** The STYLE handle, as for ATTRIB. */
    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgAttdef) target).setStyleHandle(new io.dwg.core.type.DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
