package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

public class DwgScaleList extends AbstractDwgEntity {
    private String listName;
    private List<Double> scaleFactors;
    private boolean isUnitScale;
    private int scalePaperUnits;
    private int scaleDrawingUnits;

    public DwgScaleList() {
        this.scaleFactors = new ArrayList<>();
    }

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_SCALE_LIST;
    }

    public String listName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }

    public List<Double> scaleFactors() { return scaleFactors; }

    public boolean isUnitScale() { return isUnitScale; }
    public void setUnitScale(boolean unitScale) { isUnitScale = unitScale; }

    public int scalePaperUnits() { return scalePaperUnits; }
    public void setScalePaperUnits(int scalePaperUnits) { this.scalePaperUnits = scalePaperUnits; }

    public int scaleDrawingUnits() { return scaleDrawingUnits; }
    public void setScaleDrawingUnits(int scaleDrawingUnits) { this.scaleDrawingUnits = scaleDrawingUnits; }
}
