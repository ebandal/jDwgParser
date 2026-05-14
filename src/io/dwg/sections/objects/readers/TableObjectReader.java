package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTable;
import io.dwg.sections.objects.ObjectReader;

public class TableObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_TABLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgTable table = (DwgTable) target;

        // Table name
        table.setTableName(r.readText());

        // Number of rows
        table.setNumRows(r.readBitShort());

        // Number of columns
        table.setNumColumns(r.readBitShort());

        // Row height
        table.setRowHeight(r.readBitDouble());

        // Column width
        table.setColumnWidth(r.readBitDouble());

        // Table style ID
        table.setTableStyleId(r.readBitShort());

        // Is horizontal flow
        table.setHorizontalFlow(r.readBitShort() != 0);

        // Is bidirectional flow
        table.setBidirectionalFlow(r.readBitShort() != 0);

        // Read cell contents
        int totalCells = table.numRows() * table.numColumns();
        for (int i = 0; i < totalCells && i < 1000; i++) {
            try {
                table.cellContents().add(r.readText());
            } catch (Exception e) {
                break;
            }
        }
    }
}
