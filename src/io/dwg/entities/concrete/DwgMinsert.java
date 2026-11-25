package io.dwg.entities.concrete;

import io.dwg.entities.DwgObjectType;

/**
 * MINSERT (type 8, spec §20.4.10): an INSERT repeated in a grid of rows and columns.
 */
public class DwgMinsert extends DwgInsert {
    private int columnCount = 1;
    private int rowCount = 1;
    private double columnSpacing;
    private double rowSpacing;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MINSERT; }

    /** DXF 70 */
    public int columnCount() { return columnCount; }
    /** DXF 71 */
    public int rowCount() { return rowCount; }
    /** DXF 44 */
    public double columnSpacing() { return columnSpacing; }
    /** DXF 45 */
    public double rowSpacing() { return rowSpacing; }

    /** The file holds no block name, only {@link #blockHeaderHandle()}; always "". */
    public String blockName() { return ""; }
    /** The X, Y and Z scale. */
    public double[] scale() { return new double[] {xScale(), yScale(), zScale()}; }

    public void setColumnCount(int n) { this.columnCount = n; }
    public void setRowCount(int n) { this.rowCount = n; }
    public void setColumnSpacing(double d) { this.columnSpacing = d; }
    public void setRowSpacing(double d) { this.rowSpacing = d; }
}
