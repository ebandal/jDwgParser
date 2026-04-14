package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgHatch;
import io.dwg.sections.objects.ObjectReader;

/**
 * HATCH 엔티티 리더 (타입 0x4C)
 * 닫힌 영역을 패턴이나 색상으로 채우기
 */
public class HatchObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.HATCH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgHatch hatch = (DwgHatch) target;

        // 해치 패턴 이름
        String patternName = r.readVariableText();
        hatch.setPatternName(patternName);

        // 해치 스타일
        int hatchStyle = r.getInput().readRawShort();
        hatch.setHatchStyle(hatchStyle);

        // 해치 각도 (라디안)
        double angle = r.getInput().readRawDouble();
        hatch.setAngle(angle);

        // 해치 스케일
        double scale = r.getInput().readRawDouble();
        hatch.setScale(scale);

        // 경계 경로 개수
        int numBoundaryPaths = r.getInput().readRawShort();
        hatch.setNumBoundaryPaths(numBoundaryPaths);

        // 경계 경로는 복잡하므로 개수만 저장하고 skip
        // (실제 구현에서는 경로 데이터를 읽고 파싱해야 함)
    }
}
