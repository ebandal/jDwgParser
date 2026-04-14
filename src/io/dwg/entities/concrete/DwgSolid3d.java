package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * SOLID3D 엔티티 (타입 0x26)
 * 3D 솔리드 (ACIS 기하학 데이터)
 */
public class DwgSolid3d extends AbstractDwgEntity {
    private int numModelerFormatVersion; // 모델러 포맷 버전
    private byte[] modelerGeometryData;  // 모델러 기하학 데이터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SOLID3D; }

    public int numModelerFormatVersion() { return numModelerFormatVersion; }
    public byte[] modelerGeometryData() { return modelerGeometryData; }

    public void setNumModelerFormatVersion(int version) { this.numModelerFormatVersion = version; }
    public void setModelerGeometryData(byte[] data) { this.modelerGeometryData = data; }
}
