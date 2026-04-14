package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.sections.objects.ObjectReader;

/**
 * MTEXT 엔티티 리더 (타입 0x2C)
 * 여러 줄 텍스트 (포맷, 색상, 글꼴 지원)
 */
public class MTextObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MTEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMText mtext = (DwgMText) target;

        // 위치 (3D)
        double[] location = r.read3RawDouble();
        mtext.setLocation(new Point3D(location[0], location[1], location[2]));

        // 노멀 벡터 (각도 계산용)
        double[] extrusion = r.readBitExtrusion();
        mtext.setExtrusion(extrusion);

        // 회전 각도
        double angle = r.getInput().readRawDouble();
        mtext.setAngle(angle);

        // 텍스트 박스 크기
        double width = r.getInput().readRawDouble();
        double height = r.getInput().readRawDouble();
        mtext.setWidth(width);
        mtext.setHeight(height);

        // 정렬 방식 (1-9)
        int attachmentPoint = r.getInput().readRawShort();
        mtext.setAttachmentPoint(attachmentPoint);

        // 텍스트 읽기 (버전에 따라 ASCII 또는 유니코드)
        String text = r.readVariableText();
        mtext.setText(text);

        // 스타일 이름
        String styleName = r.readVariableText();
        mtext.setStyleName(styleName);
    }
}
