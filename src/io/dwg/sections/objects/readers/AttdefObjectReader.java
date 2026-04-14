package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttdef;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTDEF 엔티티 리더 (타입 0x02)
 * 속성 정의
 */
public class AttdefObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ATTDEF.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgAttdef attdef = (DwgAttdef) target;

        // 1. insertionPoint (3RD)
        double[] pt = r.read3RawDouble();
        attdef.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        // 2. tag (TU - variable text)
        String tag = r.readVariableText();
        attdef.setTag(tag);

        // 3. prompt (TU)
        String prompt = r.readVariableText();
        attdef.setPrompt(prompt);

        // 4. defaultValue (TU)
        String defaultValue = r.readVariableText();
        attdef.setDefaultValue(defaultValue);

        // 5. textString (TU)
        String textString = r.readVariableText();
        attdef.setTextString(textString);

        // 6. flags (BS)
        int flags = r.readBitShort();
        attdef.setFlags(flags);

        // 7. justification (BS)
        int justification = r.readBitShort();
        attdef.setJustification(justification);

        // 8. textHeight (BD)
        double textHeight = r.readBitDouble();
        attdef.setTextHeight(textHeight);

        // 9. rotation (BD)
        double rotation = r.readBitDouble();
        attdef.setRotation(rotation);

        // 10. widthFactor (BD)
        double widthFactor = r.readBitDouble();
        attdef.setWidthFactor(widthFactor);

        // 11. obliquingAngle (BD)
        double obliquingAngle = r.readBitDouble();
        attdef.setObliquingAngle(obliquingAngle);

        // 12. styleName (TU)
        String styleName = r.readVariableText();
        attdef.setStyleName(styleName);
    }
}
