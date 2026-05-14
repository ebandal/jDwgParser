package io.dwg.entities;

/**
 * DWG 테이블 제어 객체(CONTROL objects) 타입 코드.
 *
 * 참고: 이 enum은 OpenDesign 스펙의 CONTROL 타입 코드를 문서화하기 위한 것입니다.
 * 현재 구현의 DwgObjectType에는 CONTROL 타입이 포함되지 않습니다.
 * 향후 CONTROL 객체 파싱이 구현되면, 이 코드들을 검증하고 통합해야 합니다.
 *
 * 실제 DWG 파일에서의 CONTROL 타입 코드는 spec doc과 다를 수 있으며,
 * 단계적 구현을 위해 별도 enum으로 분리했습니다.
 */
public enum DwgControlType {
    BLOCK_CONTROL(0x30, "블록 제어 객체"),
    LAYER_CONTROL(0x32, "레이어 제어 객체"),
    STYLE_CONTROL(0x34, "텍스트 스타일 제어 객체"),
    LTYPE_CONTROL(0x38, "선종류 제어 객체"),
    VIEW_CONTROL(0x3C, "뷰 제어 객체"),
    UCS_CONTROL(0x3E, "UCS 제어 객체"),
    VPORT_CONTROL(0x40, "뷰포트 제어 객체"),
    APPID_CONTROL(0x42, "AppID 제어 객체"),
    DIMSTYLE_CONTROL(0x44, "치수스타일 제어 객체");

    private final int typeCode;
    private final String description;

    DwgControlType(int typeCode, String description) {
        this.typeCode = typeCode;
        this.description = description;
    }

    public int typeCode() { return typeCode; }
    public String description() { return description; }

    public static DwgControlType fromCode(int code) {
        for (DwgControlType t : values()) {
            if (t.typeCode == code) return t;
        }
        return null;
    }
}
