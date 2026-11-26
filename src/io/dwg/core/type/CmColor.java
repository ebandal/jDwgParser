package io.dwg.core.type;

/**
 * DWG 색상 표현. §2.11 CMC 구조 대응. Immutable, so that equal colors can be shared: the
 * index colors 0 to 256 of {@link #fromIndex} are one instance each.
 */
public final class CmColor {
    private static final CmColor[] INDEX_COLORS = new CmColor[257];
    static {
        for (int i = 0; i < INDEX_COLORS.length; i++) {
            INDEX_COLORS[i] = new CmColor(i);
        }
    }

    private final int colorIndex;       // ACI 색상 인덱스 (0~256)
    private final int rgb;              // R2004+ RGB 값 (0xRRGGBB)
    private final byte colorType;        // 색상 타입 바이트

    public CmColor(int colorIndex) {
        this.colorIndex = colorIndex;
        this.rgb = 0;
        this.colorType = 0;
    }

    public CmColor(int colorIndex, int rgb, byte colorType) {
        this.colorIndex = colorIndex;
        this.rgb = rgb;
        this.colorType = colorType;
    }

    /**
     * ACI 인덱스 기반 생성 (0~256 are shared instances)
     */
    public static CmColor fromIndex(int index) {
        return index >= 0 && index < INDEX_COLORS.length ? INDEX_COLORS[index] : new CmColor(index);
    }

    /**
     * RGB 기반 생성 (R2004+)
     */
    public static CmColor fromRgb(int rgb) {
        return new CmColor(256, rgb, (byte)3);
    }

    /**
     * "ByLayer" 색상인지 확인 (index == 256)
     */
    public boolean isByLayer() {
        return colorIndex == 256;
    }

    /**
     * "ByBlock" 색상인지 확인 (index == 0)
     */
    public boolean isByBlock() {
        return colorIndex == 0;
    }

    /**
     * ARGB 정수로 변환
     */
    public int toArgb() {
        return 0xFF000000 | rgb;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public int getRgb() {
        return rgb;
    }

    public byte getColorType() {
        return colorType;
    }

    @Override
    public String toString() {
        return String.format("CmColor[index=%d, rgb=0x%06X]", colorIndex, rgb);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CmColor)) return false;
        CmColor cmColor = (CmColor) o;
        return colorIndex == cmColor.colorIndex && rgb == cmColor.rgb;
    }

    @Override
    public int hashCode() {
        return 31 * colorIndex + rgb;
    }
}
