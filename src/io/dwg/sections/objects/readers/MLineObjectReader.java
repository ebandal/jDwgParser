package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLine;
import io.dwg.sections.objects.ObjectReader;

/**
 * MLINE 엔티티 리더 (타입 0x2F)
 * 여러 개의 평행선
 */
public class MLineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMLine mline = (DwgMLine) target;

        // 노멀 벡터
        double[] extrusion = r.readBitExtrusion();
        mline.setExtrusion(extrusion);

        // 스타일 이름
        String styleHandle = r.readVariableText();
        mline.setStyleHandle(styleHandle);

        // 정렬 방식 (0=top, 1=middle, 2=bottom)
        int justification = r.getInput().readRawShort();
        mline.setJustification(justification);

        // 스케일
        double scale = r.getInput().readRawDouble();
        mline.setScale(scale);

        // 꼭지점 개수
        int numVertices = r.getInput().readRawShort();
        for (int i = 0; i < numVertices; i++) {
            double[] pt = r.read3BitDouble();
            mline.addVertex(new Point3D(pt[0], pt[1], pt[2]));
        }
    }
}
