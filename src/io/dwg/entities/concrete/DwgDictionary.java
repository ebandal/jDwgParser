package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DICTIONARY 엔티티 (타입 0x2A)
 * 이름-값 쌍의 집합 (DWG 문서 구조의 핵심)
 */
public class DwgDictionary extends AbstractDwgEntity {
    private int dictonaryType;           // 딕셔너리 타입
    private int duplicateRecordCloning;  // 중복 레코드 처리 방식
    private Map<String, Object> entries; // 항목들 (이름 -> 핸들)

    public DwgDictionary() {
        this.entries = new LinkedHashMap<>();
    }

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DICTIONARY; }

    public int dictionaryType() { return dictonaryType; }
    public int duplicateRecordCloning() { return duplicateRecordCloning; }
    public Map<String, Object> entries() { return entries; }

    public void setDictionaryType(int type) { this.dictonaryType = type; }
    public void setDuplicateRecordCloning(int cloning) { this.duplicateRecordCloning = cloning; }
    public void addEntry(String name, Object value) { this.entries.put(name, value); }
    public Object getEntry(String name) { return entries.get(name); }
}
