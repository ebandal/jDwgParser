package io.dwg.entities;

/**
 * 스펙 §20에서 정의된 표준 객체 타입 코드.
 */
public enum DwgObjectType {
    UNUSED(0x00),
    TEXT(0x01),
    ATTDEF(0x02),
    ATTRIB(0x03),
    SEQEND(0x04),
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
    MTEXT(0x2C),
    LEADER(0x2D),
    TOLERANCE(0x2E),
    MLINE(0x2F),
    BLOCK_HEADER(0x30),
    BLOCK_END(0x31),
    LTYPE(0x32),
    LAYER(0x33),
    STYLE(0x34),
    VIEW(0x36),
    UCS(0x37),
    VPORT(0x38),
    APPID(0x39),
    DIMSTYLE(0x3A),
    VP_ENT_HDR(0x3B),
    GROUP(0x3C),
    MLINESTYLE(0x3D),
    OLE2FRAME(0x3E),
    LONG_TRANSACTION(0x40),
    LWPLINE(0x4B),
    HATCH(0x4C),
    XRECORD(0x4D),
    PLACEHOLDER(0x4E),
    VBA_PROJECT(0x4F),
    LAYOUT(0x50),
    UNKNOWN(-1);

    private final int typeCode;

    DwgObjectType(int typeCode) {
        this.typeCode = typeCode;
    }

    public int typeCode() {
        return typeCode;
    }

    public static DwgObjectType fromCode(int code) {
        for (DwgObjectType t : values()) {
            if (t.typeCode == code) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
