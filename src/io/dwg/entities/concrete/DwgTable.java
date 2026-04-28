package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

public class DwgTable extends AbstractDwgEntity {
    private String tableName;
    private int numRows;
    private int numColumns;
    private double rowHeight;
    private double columnWidth;
    private List<String> cellContents;
    private int tableStyleId;
    private boolean isHorizontalFlow;
    private boolean isBidirectionalFlow;

    public DwgTable() {
        this.cellContents = new ArrayList<>();
    }

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_TABLE;
    }

    public String tableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public int numRows() { return numRows; }
    public void setNumRows(int numRows) { this.numRows = numRows; }

    public int numColumns() { return numColumns; }
    public void setNumColumns(int numColumns) { this.numColumns = numColumns; }

    public double rowHeight() { return rowHeight; }
    public void setRowHeight(double rowHeight) { this.rowHeight = rowHeight; }

    public double columnWidth() { return columnWidth; }
    public void setColumnWidth(double columnWidth) { this.columnWidth = columnWidth; }

    public List<String> cellContents() { return cellContents; }

    public int tableStyleId() { return tableStyleId; }
    public void setTableStyleId(int tableStyleId) { this.tableStyleId = tableStyleId; }

    public boolean isHorizontalFlow() { return isHorizontalFlow; }
    public void setHorizontalFlow(boolean horizontalFlow) { isHorizontalFlow = horizontalFlow; }

    public boolean isBidirectionalFlow() { return isBidirectionalFlow; }
    public void setBidirectionalFlow(boolean bidirectionalFlow) { isBidirectionalFlow = bidirectionalFlow; }
}
