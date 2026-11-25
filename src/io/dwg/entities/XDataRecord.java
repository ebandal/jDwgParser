package io.dwg.entities;

/**
 * 확장 엔티티 데이터(XData) 레코드 하나. 그룹 코드는 DXF와 같다.
 * <ul>
 *   <li>1001: 애플리케이션 이름(String). 애플리케이션마다 데이터 앞에 하나씩 있다.
 *       파일에 그 APPID가 없으면 핸들({@link io.dwg.core.type.DwgHandleRef})</li>
 *   <li>1000 String, 1002 "{" 또는 "}", 1003·1005 {@link io.dwg.core.type.DwgHandleRef},
 *       1004 byte[], 1010~1013 {@link io.dwg.core.type.Point3D}, 1040~1042 Double,
 *       1070 Short, 1071 Integer</li>
 *   <li>-1: 읽지 못한 나머지 바이트(byte[]). 알 수 없는 코드가 나오면 그 뒤를 이렇게 보관한다.</li>
 * </ul>
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
