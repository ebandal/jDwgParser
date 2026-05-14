package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * ATTDEF 엔티티 (타입 0x02)
 * 속성 정의 (INSERT할 때 속성 설정에 사용)
 */
public class DwgAttdef extends AbstractDwgEntity {
    private Point3D insertionPoint;
    private String tag;              // 속성 태그명
    private String prompt;           // 속성 입력 프롬프트
    private String defaultValue;     // 기본값
    private String textString;       // 텍스트 내용
    private int flags;               // 속성 플래그 (보이기, 사용자 입력, 미리 설정 등)
    private int justification;       // 텍스트 정렬
    private double textHeight;       // 텍스트 높이
    private double rotation;         // 회전각
    private double widthFactor;      // 가로 배율
    private double obliquingAngle;   // 기울기각
    private String styleName;        // 스타일명

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ATTDEF; }

    public Point3D insertionPoint() { return insertionPoint; }
    public String tag() { return tag; }
    public String prompt() { return prompt; }
    public String defaultValue() { return defaultValue; }
    public String textString() { return textString; }
    public int flags() { return flags; }
    public int justification() { return justification; }
    public double textHeight() { return textHeight; }
    public double rotation() { return rotation; }
    public double widthFactor() { return widthFactor; }
    public double obliquingAngle() { return obliquingAngle; }
    public String styleName() { return styleName; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setTag(String tag) { this.tag = tag; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public void setTextString(String textString) { this.textString = textString; }
    public void setFlags(int flags) { this.flags = flags; }
    public void setJustification(int justification) { this.justification = justification; }
    public void setTextHeight(double textHeight) { this.textHeight = textHeight; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public void setWidthFactor(double widthFactor) { this.widthFactor = widthFactor; }
    public void setObliquingAngle(double obliquingAngle) { this.obliquingAngle = obliquingAngle; }
    public void setStyleName(String styleName) { this.styleName = styleName; }

    public boolean isInvisible() { return (flags & 0x01) != 0; }
    public boolean isConstant() { return (flags & 0x02) != 0; }
    public boolean isVerified() { return (flags & 0x04) != 0; }
    public boolean isPreset() { return (flags & 0x08) != 0; }
}
