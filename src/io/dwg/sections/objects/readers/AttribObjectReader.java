package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.sections.objects.ObjectReader;

/**
 * ATTRIB 엔티티 리더 (타입 0x03)
 * INSERT에 포함된 속성값
 */
public class AttribObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ATTRIB.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgAttrib attrib = (DwgAttrib) target;

        // 위치 (3D)
        double[] location = r.read3RawDouble();
        attrib.setLocation(new Point3D(location[0], location[1], location[2]));

        // 높이
        double height = r.getInput().readRawDouble();
        attrib.setHeight(height);

        // 속성값 (텍스트)
        String text = r.readVariableText();
        attrib.setText(text);

        // 회전 각도
        double angle = r.getInput().readRawDouble();
        attrib.setAngle(angle);

        // 속성 이름 (태그)
        String tag = r.readVariableText();
        attrib.setTag(tag);

        // 스타일 이름
        String styleName = r.readVariableText();
        attrib.setStyleName(styleName);
    }
}
