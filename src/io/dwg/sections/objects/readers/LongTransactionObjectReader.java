package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLongTransaction;
import io.dwg.sections.objects.ObjectReader;

/**
 * LONG_TRANSACTION (장기 트랜잭션) 특수 객체 ObjectReader.
 * 타입 0x40
 */
public class LongTransactionObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LONG_TRANSACTION.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLongTransaction txn = (DwgLongTransaction) target;

        txn.setTransactionId(r.readBitLong());
        txn.setStatus(r.readBitShort());
    }
}
