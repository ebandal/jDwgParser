package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLeader;
import io.dwg.sections.objects.ObjectReader;

/**
 * LEADER 엔티티 리더 (타입 0x2D)
 * 주석을 가리키는 선 (화살표 포함)
 */
public class LeaderObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LEADER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLeader leader = (DwgLeader) target;

        // 노멀 벡터
        double[] extrusion = r.readBitExtrusion();
        leader.setExtrusion(extrusion);

        // 스타일 이름
        String styleName = r.readVariableText();
        leader.setStyleName(styleName);

        // 화살표 모양
        int arrow = r.getInput().readRawShort();
        leader.setArrow(arrow);

        // 점의 개수
        int numPoints = r.getInput().readRawShort();
        for (int i = 0; i < numPoints; i++) {
            double[] pt = r.read3BitDouble();
            leader.addPoint(new Point3D(pt[0], pt[1], pt[2]));
        }
    }
}
