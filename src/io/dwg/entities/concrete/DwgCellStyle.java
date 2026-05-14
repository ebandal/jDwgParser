package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgCellStyle extends AbstractDwgEntity {
    private String cellStyleName;
    private String description;
    private int cellBorderWidth;
    private int cellBorderStyle;
    private int cellBackgroundColor;
    private int cellTextColor;
    private int cellAlignment;
    private double cellRotation;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_CELLSTYLE;
    }

    public String cellStyleName() { return cellStyleName; }
    public void setCellStyleName(String cellStyleName) { this.cellStyleName = cellStyleName; }

    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int cellBorderWidth() { return cellBorderWidth; }
    public void setCellBorderWidth(int cellBorderWidth) { this.cellBorderWidth = cellBorderWidth; }

    public int cellBorderStyle() { return cellBorderStyle; }
    public void setCellBorderStyle(int cellBorderStyle) { this.cellBorderStyle = cellBorderStyle; }

    public int cellBackgroundColor() { return cellBackgroundColor; }
    public void setCellBackgroundColor(int cellBackgroundColor) { this.cellBackgroundColor = cellBackgroundColor; }

    public int cellTextColor() { return cellTextColor; }
    public void setCellTextColor(int cellTextColor) { this.cellTextColor = cellTextColor; }

    public int cellAlignment() { return cellAlignment; }
    public void setCellAlignment(int cellAlignment) { this.cellAlignment = cellAlignment; }

    public double cellRotation() { return cellRotation; }
    public void setCellRotation(double cellRotation) { this.cellRotation = cellRotation; }
}
