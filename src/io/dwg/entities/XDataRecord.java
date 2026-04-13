package io.dwg.entities;

/**
 * 확장 엔티티 데이터(XData) 레코드.
 */
public class XDataRecord {
    private final int groupCode;
    private final Object value;

    public XDataRecord(int groupCode, Object value) {
        this.groupCode = groupCode;
        this.value = value;
    }

    public int groupCode() { return groupCode; }
    public Object value() { return value; }

    @Override
    public String toString() {
        return "XDataRecord[code=" + groupCode + ", value=" + value + "]";
    }
}
