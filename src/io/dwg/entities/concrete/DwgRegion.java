package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * REGION 엔티티 (타입 0x25)
 * 영역 (닫힌 영역의 윤곽선으로 표현)
 */
public class DwgRegion extends AbstractDwgEntity {
    private int numModelerFormatVersion; // 모델러 포맷 버전
    private byte[] modelerGeometryData;  // 모델러 기하학 데이터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.REGION; }

    public int numModelerFormatVersion() { return numModelerFormatVersion; }
    public byte[] modelerGeometryData() { return modelerGeometryData; }

    public void setNumModelerFormatVersion(int version) { this.numModelerFormatVersion = version; }
    public void setModelerGeometryData(byte[] data) { this.modelerGeometryData = data; }
}
