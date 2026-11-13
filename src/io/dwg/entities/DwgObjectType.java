package io.dwg.entities;

/**
 * 객체 타입.
 *
 * <p>고정 타입은 스펙의 "Object types" 표에 있는 타입 코드를 가진다.
 * 그 밖의 타입은 파일마다 Classes 섹션이 500 이상의 번호를 부여하므로 고정된 코드가 없고,
 * 클래스의 DXF 이름으로 구분한다. 이런 타입의 {@link #typeCode()}는 파일에 나타나지 않는
 * 내부 값(0x10000 이상)이며, {@link #fromDxfName(String)}으로 찾는다.
 */
public enum DwgObjectType {
    UNUSED(0x00),
    TEXT(0x01),
    ATTRIB(0x02),
    ATTDEF(0x03),
    BLOCK(0x04),
    ENDBLK(0x05),
    SEQEND(0x06),
    INSERT(0x07),
    MINSERT(0x08),
    VERTEX_2D(0x0A),
    VERTEX_3D(0x0B),
    VERTEX_MESH(0x0C),
    VERTEX_PFACE(0x0D),
    VERTEX_PFACE_FACE(0x0E),
    POLYLINE_2D(0x0F),
    POLYLINE_3D(0x10),
    ARC(0x11),
    CIRCLE(0x12),
    LINE(0x13),
    DIMENSION_ORDINATE(0x14),
    DIMENSION_LINEAR(0x15),
    DIMENSION_ALIGNED(0x16),
    DIMENSION_ANG_3PT(0x17),
    DIMENSION_ANG_2LN(0x18),
    DIMENSION_RADIUS(0x19),
    DIMENSION_DIAMETER(0x1A),
    POINT(0x1B),
    FACE3D(0x1C),
    POLYLINE_PFACE(0x1D),
    POLYLINE_MESH(0x1E),
    SOLID(0x1F),
    TRACE(0x20),
    SHAPE(0x21),
    VIEWPORT(0x22),
    ELLIPSE(0x23),
    SPLINE(0x24),
    REGION(0x25),
    SOLID3D(0x26),
    BODY(0x27),
    RAY(0x28),
    XLINE(0x29),
    DICTIONARY(0x2A),
    OLEFRAME(0x2B),
    MTEXT(0x2C),
    LEADER(0x2D),
    TOLERANCE(0x2E),
    MLINE(0x2F),
    BLOCK_CONTROL(0x30),
    BLOCK_HEADER(0x31),
    LAYER_CONTROL(0x32),
    LAYER(0x33),
    STYLE_CONTROL(0x34),
    STYLE(0x35),
    LTYPE_CONTROL(0x38),
    LTYPE(0x39),
    VIEW_CONTROL(0x3C),
    VIEW(0x3D),
    UCS_CONTROL(0x3E),
    UCS(0x3F),
    VPORT_CONTROL(0x40),
    VPORT(0x41),
    APPID_CONTROL(0x42),
    APPID(0x43),
    DIMSTYLE_CONTROL(0x44),
    DIMSTYLE(0x45),
    VP_ENT_HDR_CONTROL(0x46),
    VP_ENT_HDR(0x47),
    GROUP(0x48),
    MLINESTYLE(0x49),
    OLE2FRAME(0x4A, "OLE2FRAME"),
    DUMMY(0x4B),
    LONG_TRANSACTION(0x4C),
    LWPLINE(0x4D, "LWPOLYLINE"),
    HATCH(0x4E, "HATCH"),
    XRECORD(0x4F, "XRECORD"),
    PLACEHOLDER(0x50, "ACDBPLACEHOLDER"),
    VBA_PROJECT(0x51, "VBA_PROJECT"),
    LAYOUT(0x52, "LAYOUT"),
    ACAD_PROXY_ENTITY(0x1F2),
    ACAD_PROXY_OBJECT(0x1F3),

    // Classes 섹션으로 정의되는 타입 : 코드는 내부 값, DXF 이름으로 찾는다
    IMAGE(0x10000, "IMAGE"),
    WIPEOUT(0x10001, "WIPEOUT"),
    XREF(0x10002),
    UNDERLAY(0x10003, "PDFUNDERLAY", "DWFUNDERLAY", "DGNUNDERLAY"),
    SURFACE(0x10004, "PLANESURFACE", "EXTRUDEDSURFACE", "LOFTEDSURFACE", "REVOLVEDSURFACE", "SWEPTSURFACE", "NURBSURFACE"),
    MESH(0x10005, "MESH"),
    SCALE(0x10006, "SCALE"),
    VISUALSTYLE(0x10007, "VISUALSTYLE"),
    ACAD_FIELD(0x10008, "FIELD"),
    ACAD_DICTIONARYVAR(0x10009, "DICTIONARYVAR"),
    ACAD_TABLE(0x1000A, "ACAD_TABLE"),
    ACAD_SCALE_LIST(0x1000B),
    ACAD_TABLESTYLE(0x1000C, "TABLESTYLE"),
    ACAD_CELLSTYLE(0x1000D, "CELLSTYLEMAP"),
    ACAD_PLOTSTYLE(0x1000E),
    ACAD_MATERIAL(0x1000F, "MATERIAL"),
    ACAD_DATASOURCE(0x10010),
    ACAD_PERSSUBENTMANAGER(0x10011, "ACDBPERSSUBENTMANAGER"),
    UNKNOWN(-1);

    /** 이 값 이상의 타입 코드는 Classes 섹션의 클래스 번호이다. */
    public static final int FIRST_CLASS_NUMBER = 500;

    private final int typeCode;
    private final String[] dxfNames;

    DwgObjectType(int typeCode, String... dxfNames) {
        this.typeCode = typeCode;
        this.dxfNames = dxfNames;
    }

    public int typeCode() {
        return typeCode;
    }

    /** 파일에 기록된 타입 코드로 고정 타입을 찾는다. 클래스 번호(500 이상)는 UNKNOWN을 돌려준다. */
    public static DwgObjectType fromCode(int code) {
        if (code >= 0 && code < FIRST_CLASS_NUMBER) {
            for (DwgObjectType t : values()) {
                if (t.typeCode == code) {
                    return t;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Classes 섹션의 DXF 이름으로 타입을 찾는다.
     * 고정 코드가 있는 타입도 오래된 파일이나 일부 파일에서는 클래스로 정의되어 있으므로 이름을 가진다.
     */
    public static DwgObjectType fromDxfName(String dxfName) {
        if (dxfName != null) {
            for (DwgObjectType t : values()) {
                for (String name : t.dxfNames) {
                    if (name.equalsIgnoreCase(dxfName)) {
                        return t;
                    }
                }
            }
        }
        return UNKNOWN;
    }
}
