package io.dwg.core.version;

import io.dwg.core.exception.DwgVersionException;

/**
 * 지원하는 모든 DWG 버전 열거.
 * 파일 헤더 버전 문자열과 매핑.
 */
public enum DwgVersion {
    R13("AC1012"),
    R14("AC1014"),
    R2000("AC1015"),
    R2004("AC1018"),
    R2007("AC1021"),
    R2010("AC1024"),
    R2013("AC1027"),
    R2018("AC1032");

    private final String versionString;

    DwgVersion(String versionString) {
        this.versionString = versionString;
    }

    public String versionString() {
        return versionString;
    }

    /**
     * 같은 버전인지 확인
     */
    public boolean only(DwgVersion ver) {
        return this == ver;
    }

    /**
     * 두 버전 사이에 있는지 확인 (포함)
     */
    public boolean between(DwgVersion ver1, DwgVersion ver2) {
        int thisOrd = this.ordinal();
        int ord1 = Math.min(ver1.ordinal(), ver2.ordinal());
        int ord2 = Math.max(ver1.ordinal(), ver2.ordinal());
        return thisOrd >= ord1 && thisOrd <= ord2;
    }

    /**
     * 특정 버전보다 이후인지 확인
     */
    public boolean from(DwgVersion ver) {
        return this.ordinal() >= ver.ordinal();
    }

    /**
     * 특정 버전 이전인지 확인
     */
    public boolean until(DwgVersion ver) {
        return this.ordinal() <= ver.ordinal();
    }

    /**
     * 특정 버전과 같거나 이후인지 확인
     */
    public boolean isAtLeast(DwgVersion other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * R2007 이후인지 확인 (LZ77 압축, UTF-16 적용)
     */
    public boolean isR2007OrLater() {
        return this.isAtLeast(R2007);
    }

    /**
     * R2004 이후인지 확인 (섹션 맵 구조)
     */
    public boolean isR2004OrLater() {
        return this.isAtLeast(R2004);
    }

    /**
     * UTF-16 인코딩 사용 여부
     */
    public boolean usesUnicode() {
        return this.isR2007OrLater();
    }

    /**
     * 버전 문자열 → DwgVersion 변환
     */
    public static DwgVersion fromString(String s) {
        if (s == null) {
            throw new DwgVersionException("null");
        }
        for (DwgVersion v : values()) {
            if (v.versionString.equals(s)) {
                return v;
            }
        }
        throw new DwgVersionException(s);
    }
}
