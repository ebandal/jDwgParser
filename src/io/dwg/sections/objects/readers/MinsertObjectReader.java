package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMinsert;
import io.dwg.sections.objects.ObjectReader;

/**
 * MINSERT 엔티티 리더 (타입 0x08)
 * 다중 삽입 (배열로 배치된 블록 참조)
 */
public class MinsertObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.MINSERT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMinsert minsert = (DwgMinsert) target;

        // 1. blockName (TU)
        String blockName = r.readVariableText();
        minsert.setBlockName(blockName);

        // 2. insertionPoint (3RD)
        double[] pt = r.read3RawDouble();
        minsert.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        // 3. scale (3RD or individual BD values)
        double[] scale = r.read3BitDouble();
        minsert.setScale(scale);

        // 4. rotation (BD)
        double rotation = r.readBitDouble();
        minsert.setRotation(rotation);

        // 5. rowCount (BL)
        int rowCount = r.readBitLong();
        minsert.setRowCount(rowCount);

        // 6. columnCount (BL)
        int columnCount = r.readBitLong();
        minsert.setColumnCount(columnCount);

        // 7. rowSpacing (BD)
        double rowSpacing = r.readBitDouble();
        minsert.setRowSpacing(rowSpacing);

        // 8. columnSpacing (BD)
        double columnSpacing = r.readBitDouble();
        minsert.setColumnSpacing(columnSpacing);

        // 9. extrusion (BE)
        double[] extrusion = r.readBitExtrusion();
        minsert.setExtrusion(extrusion);
    }
}
