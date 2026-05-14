package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * MTEXT 엔티티 (타입 0x2C)
 * 여러 줄 텍스트 (포맷, 색상, 글꼴 지원)
 */
public class DwgMText extends AbstractDwgEntity {
    private Point3D location;
    private double width;  // 텍스트 박스 폭
    private double height; // 텍스트 박스 높이
    private String text;
    private int attachmentPoint;  // 정렬 방식 (1-9)
    private String styleName;
    private double angle;
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MTEXT; }

    public Point3D location() { return location; }
    public double width() { return width; }
    public double height() { return height; }
    public String text() { return text; }
    public int attachmentPoint() { return attachmentPoint; }
    public String styleName() { return styleName; }
    public double angle() { return angle; }
    public double[] extrusion() { return extrusion; }

    public void setLocation(Point3D location) { this.location = location; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setText(String text) { this.text = text; }
    public void setAttachmentPoint(int attachmentPoint) { this.attachmentPoint = attachmentPoint; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
