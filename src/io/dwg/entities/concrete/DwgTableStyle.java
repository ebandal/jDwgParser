package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgTableStyle extends AbstractDwgEntity {
    private String tableName;
    private String description;
    private int tableStyleFlags;
    private int rowHeight;
    private int columnWidth;
    private int titleRowHeight;
    private int headerRowHeight;
    private int dataRowHeight;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_TABLESTYLE;
    }

    public String tableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int tableStyleFlags() { return tableStyleFlags; }
    public void setTableStyleFlags(int tableStyleFlags) { this.tableStyleFlags = tableStyleFlags; }

    public int rowHeight() { return rowHeight; }
    public void setRowHeight(int rowHeight) { this.rowHeight = rowHeight; }

    public int columnWidth() { return columnWidth; }
    public void setColumnWidth(int columnWidth) { this.columnWidth = columnWidth; }

    public int titleRowHeight() { return titleRowHeight; }
    public void setTitleRowHeight(int titleRowHeight) { this.titleRowHeight = titleRowHeight; }

    public int headerRowHeight() { return headerRowHeight; }
    public void setHeaderRowHeight(int headerRowHeight) { this.headerRowHeight = headerRowHeight; }

    public int dataRowHeight() { return dataRowHeight; }
    public void setDataRowHeight(int dataRowHeight) { this.dataRowHeight = dataRowHeight; }
}
