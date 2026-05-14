package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * LONG_TRANSACTION 특수 객체 (타입 0x40)
 * 장기 트랜잭션 기록
 */
public class DwgLongTransaction extends AbstractDwgObject implements DwgNonEntityObject {
    private long transactionId;
    private int status;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LONG_TRANSACTION; }

    @Override
    public boolean isEntity() { return false; }

    public long transactionId() { return transactionId; }
    public int status() { return status; }

    public void setTransactionId(long id) { this.transactionId = id; }
    public void setStatus(int status) { this.status = status; }
}
