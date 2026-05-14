package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * VISUALSTYLE 엔티티 - 비주얼 스타일 (R2007+)
 */
public class DwgVisualStyle extends AbstractDwgEntity {
    private String styleName;
    private String description;
    private int styleType;  // 0=Basic, 1=Advanced
    private double faceOpacity;
    private double edgeOpacity;
    private int faceColorMode;
    private int edgeColorMode;
    private boolean isDisplayEdges;
    private boolean isShowSilhouettes;
    private int silhouetteWidth;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VISUALSTYLE; }

    public String styleName() { return styleName; }
    public String description() { return description; }
    public int styleType() { return styleType; }
    public double faceOpacity() { return faceOpacity; }
    public double edgeOpacity() { return edgeOpacity; }
    public int faceColorMode() { return faceColorMode; }
    public int edgeColorMode() { return edgeColorMode; }
    public boolean isDisplayEdges() { return isDisplayEdges; }
    public boolean isShowSilhouettes() { return isShowSilhouettes; }
    public int silhouetteWidth() { return silhouetteWidth; }

    public void setStyleName(String styleName) { this.styleName = styleName; }
    public void setDescription(String description) { this.description = description; }
    public void setStyleType(int styleType) { this.styleType = styleType; }
    public void setFaceOpacity(double faceOpacity) { this.faceOpacity = faceOpacity; }
    public void setEdgeOpacity(double edgeOpacity) { this.edgeOpacity = edgeOpacity; }
    public void setFaceColorMode(int faceColorMode) { this.faceColorMode = faceColorMode; }
    public void setEdgeColorMode(int edgeColorMode) { this.edgeColorMode = edgeColorMode; }
    public void setIsDisplayEdges(boolean isDisplayEdges) { this.isDisplayEdges = isDisplayEdges; }
    public void setIsShowSilhouettes(boolean isShowSilhouettes) { this.isShowSilhouettes = isShowSilhouettes; }
    public void setSilhouetteWidth(int silhouetteWidth) { this.silhouetteWidth = silhouetteWidth; }
}
