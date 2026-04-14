package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * XRECORD 엔티티 (타입 0x4D)
 * 확장 데이터 레코드 (임의의 응용 프로그램 데이터 저장)
 */
public class DwgXrecord extends AbstractDwgEntity {
    private int recordType;              // 레코드 타입
    private byte[] recordData;           // 레코드 데이터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.XRECORD; }

    public int recordType() { return recordType; }
    public byte[] recordData() { return recordData; }

    public void setRecordType(int type) { this.recordType = type; }
    public void setRecordData(byte[] data) { this.recordData = data; }
}
