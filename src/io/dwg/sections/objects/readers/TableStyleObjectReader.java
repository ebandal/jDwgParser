package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTableStyle;
import io.dwg.sections.objects.ObjectReader;

public class TableStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_TABLESTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgTableStyle style = (DwgTableStyle) target;

        // Table style name
        style.setTableName(r.readText());

        // Description
        style.setDescription(r.readText());

        // Table style flags
        style.setTableStyleFlags(r.readBitShort());

        // Row height
        style.setRowHeight(r.readBitShort());

        // Column width
        style.setColumnWidth(r.readBitShort());

        // Title row height
        style.setTitleRowHeight(r.readBitShort());

        // Header row height
        style.setHeaderRowHeight(r.readBitShort());

        // Data row height
        style.setDataRowHeight(r.readBitShort());
    }
}
