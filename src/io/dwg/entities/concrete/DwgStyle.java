package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * STYLE 테이블 엔트리 (타입 0x34)
 * 문자 스타일 정의 (폰트, 크기, 각도 등)
 */
public class DwgStyle extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private String fontFilename = "";
    private String bigFontFilename = "";
    private double width = 1.0;
    private double oblique;
    private int flags;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.STYLE; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public String fontFilename() { return fontFilename; }
    public String bigFontFilename() { return bigFontFilename; }
    public double width() { return width; }
    public double oblique() { return oblique; }
    public int flags() { return flags; }

    public void setName(String name) { this.name = name; }
    public void setFontFilename(String filename) { this.fontFilename = filename; }
    public void setBigFontFilename(String filename) { this.bigFontFilename = filename; }
    public void setWidth(double width) { this.width = width; }
    public void setOblique(double oblique) { this.oblique = oblique; }
    public void setFlags(int flags) { this.flags = flags; }
}
