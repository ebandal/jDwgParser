package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgShape;
import io.dwg.sections.objects.ObjectReader;

/**
 * SHAPE 엔티티 리더 (타입 0x21)
 * 형태 파일(.SHX)에 정의된 모양
 */
public class ShapeObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SHAPE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgShape shape = (DwgShape) target;

        // 삽입 위치 (3D)
        double[] insertionPoint = r.read3RawDouble();
        shape.setInsertionPoint(new Point3D(insertionPoint[0], insertionPoint[1], insertionPoint[2]));

        // 스케일
        double scale = r.getInput().readRawDouble();
        shape.setScale(scale);

        // 회전 각도
        double angle = r.getInput().readRawDouble();
        shape.setAngle(angle);

        // 형태 이름
        String shapeName = r.readVariableText();
        shape.setShapeName(shapeName);

        // 노멀 벡터
        double[] extrusion = r.readBitExtrusion();
        shape.setExtrusion(extrusion);
    }
}
