package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgPlotStyle extends AbstractDwgEntity {
    private String plotStyleName;
    private String description;
    private int lineWeightValue;
    private int lineTypeValue;
    private double transparencyValue;
    private boolean isAdaptive;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_PLOTSTYLE;
    }

    public String plotStyleName() { return plotStyleName; }
    public void setPlotStyleName(String plotStyleName) { this.plotStyleName = plotStyleName; }

    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int lineWeightValue() { return lineWeightValue; }
    public void setLineWeightValue(int lineWeightValue) { this.lineWeightValue = lineWeightValue; }

    public int lineTypeValue() { return lineTypeValue; }
    public void setLineTypeValue(int lineTypeValue) { this.lineTypeValue = lineTypeValue; }

    public double transparencyValue() { return transparencyValue; }
    public void setTransparencyValue(double transparencyValue) { this.transparencyValue = transparencyValue; }

    public boolean isAdaptive() { return isAdaptive; }
    public void setAdaptive(boolean adaptive) { isAdaptive = adaptive; }
}
