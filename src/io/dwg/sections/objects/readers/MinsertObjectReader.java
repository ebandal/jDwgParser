package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMinsert;
import io.dwg.sections.objects.EntityHeaderReader;

/**
 * MINSERT (type 8), spec §20.4.10: the fields of INSERT, then number of columns BS, number of
 * rows BS, column spacing BD, row spacing BD. The handles are those of INSERT.
 */
public class MinsertObjectReader extends InsertObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MINSERT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgMinsert minsert = (DwgMinsert) target;
        readInsertFields(minsert, r, v);
        minsert.setColumnCount(r.readBitShort());
        minsert.setRowCount(r.readBitShort());
        minsert.setColumnSpacing(r.readBitDouble());
        minsert.setRowSpacing(r.readBitDouble());
    }
}
