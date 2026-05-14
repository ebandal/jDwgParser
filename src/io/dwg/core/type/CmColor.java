package io.dwg.core.type;

/**
 * DWG 색상 표현. §2.11 CMC 구조 대응.
 */
public class CmColor {
    private int colorIndex;       // ACI 색상 인덱스 (0~256)
    private int rgb;              // R2004+ RGB 값 (0xRRGGBB)
    private byte colorType;        // 색상 타입 바이트
    private String colorName;      // 색상 이름 (옵션)
    private String bookName;       // 색상 책 이름 (옵션)

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
     * ACI 인덱스 기반 생성
     */
    public static CmColor fromIndex(int index) {
        return new CmColor(index);
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

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    public int getRgb() {
        return rgb;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb;
    }

    public byte getColorType() {
        return colorType;
    }

    public void setColorType(byte colorType) {
        this.colorType = colorType;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
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
