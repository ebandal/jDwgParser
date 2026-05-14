package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * ATTRIB 엔티티 (타입 0x03)
 * INSERT에 포함된 속성값
 */
public class DwgAttrib extends AbstractDwgEntity {
    private Point3D location;
    private String text;  // 속성값
    private String tag;   // 속성 이름
    private String styleName;
    private double angle;
    private double height;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ATTRIB; }

    public Point3D location() { return location; }
    public String text() { return text; }
    public String tag() { return tag; }
    public String styleName() { return styleName; }
    public double angle() { return angle; }
    public double height() { return height; }

    public void setLocation(Point3D location) { this.location = location; }
    public void setText(String text) { this.text = text; }
    public void setTag(String tag) { this.tag = tag; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setHeight(double height) { this.height = height; }
}
