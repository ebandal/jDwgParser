package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgCellStyle;
import io.dwg.sections.objects.ObjectReader;

public class CellStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_CELLSTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgCellStyle style = (DwgCellStyle) target;

        // Cell style name
        style.setCellStyleName(r.readText());

        // Description
        style.setDescription(r.readText());

        // Cell border width
        style.setCellBorderWidth(r.readBitShort());

        // Cell border style
        style.setCellBorderStyle(r.readBitShort());

        // Cell background color
        style.setCellBackgroundColor(r.readBitShort());

        // Cell text color
        style.setCellTextColor(r.readBitShort());

        // Cell alignment
        style.setCellAlignment(r.readBitShort());

        // Cell rotation
        style.setCellRotation(r.readBitDouble());
    }
}
