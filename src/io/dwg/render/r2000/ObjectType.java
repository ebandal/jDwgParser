package io.dwg.render.r2000;

import java.util.Map;

/** Fixed object types (DWG specification chapter 20, "Object types"). */
public class ObjectType {
    private static final String[] NAMES = new String[0x53];
    static {
        String[] n = NAMES;
        n[0x01] = "TEXT";               n[0x02] = "ATTRIB";             n[0x03] = "ATTDEF";
        n[0x04] = "BLOCK";              n[0x05] = "ENDBLK";             n[0x06] = "SEQEND";
        n[0x07] = "INSERT";             n[0x08] = "MINSERT";
        n[0x0A] = "VERTEX_2D";          n[0x0B] = "VERTEX_3D";          n[0x0C] = "VERTEX_MESH";
        n[0x0D] = "VERTEX_PFACE";       n[0x0E] = "VERTEX_PFACE_FACE";  n[0x0F] = "POLYLINE_2D";
        n[0x10] = "POLYLINE_3D";        n[0x11] = "ARC";                n[0x12] = "CIRCLE";
        n[0x13] = "LINE";               n[0x14] = "DIMENSION_ORDINATE"; n[0x15] = "DIMENSION_LINEAR";
        n[0x16] = "DIMENSION_ALIGNED";  n[0x17] = "DIMENSION_ANG3PT";   n[0x18] = "DIMENSION_ANG2LN";
        n[0x19] = "DIMENSION_RADIUS";   n[0x1A] = "DIMENSION_DIAMETER"; n[0x1B] = "POINT";
        n[0x1C] = "3DFACE";             n[0x1D] = "POLYLINE_PFACE";     n[0x1E] = "POLYLINE_MESH";
        n[0x1F] = "SOLID";              n[0x20] = "TRACE";              n[0x21] = "SHAPE";
        n[0x22] = "VIEWPORT";           n[0x23] = "ELLIPSE";            n[0x24] = "SPLINE";
        n[0x25] = "REGION";             n[0x26] = "3DSOLID";            n[0x27] = "BODY";
        n[0x28] = "RAY";                n[0x29] = "XLINE";              n[0x2A] = "DICTIONARY";
        n[0x2B] = "OLEFRAME";           n[0x2C] = "MTEXT";              n[0x2D] = "LEADER";
        n[0x2E] = "TOLERANCE";          n[0x2F] = "MLINE";              n[0x30] = "BLOCK_CONTROL";
        n[0x31] = "BLOCK_HEADER";       n[0x32] = "LAYER_CONTROL";      n[0x33] = "LAYER";
        n[0x34] = "STYLE_CONTROL";      n[0x35] = "STYLE";              n[0x38] = "LTYPE_CONTROL";
        n[0x39] = "LTYPE";              n[0x3C] = "VIEW_CONTROL";       n[0x3D] = "VIEW";
        n[0x3E] = "UCS_CONTROL";        n[0x3F] = "UCS";                n[0x40] = "VPORT_CONTROL";
        n[0x41] = "VPORT";              n[0x42] = "APPID_CONTROL";      n[0x43] = "APPID";
        n[0x44] = "DIMSTYLE_CONTROL";   n[0x45] = "DIMSTYLE";           n[0x46] = "VP_ENT_HDR_CONTROL";
        n[0x47] = "VP_ENT_HDR";         n[0x48] = "GROUP";              n[0x49] = "MLINESTYLE";
        n[0x4A] = "OLE2FRAME";          n[0x4B] = "DUMMY";              n[0x4C] = "LONG_TRANSACTION";
        n[0x4D] = "LWPOLYLINE";         n[0x4E] = "HATCH";              n[0x4F] = "XRECORD";
        n[0x50] = "ACDBPLACEHOLDER";    n[0x51] = "VBA_PROJECT";        n[0x52] = "LAYOUT";
    }

    public static final int CLASS_BASE = 500;
    public static final int ITEM_CLASS_ENTITY = 0x1F2;

    /** Name of a type; types >= 500 are looked up in the class section. */
    public static String nameOf(int type, Map<Integer, DrawingClass> classes) {
        if (type >= 0 && type < NAMES.length && NAMES[type] != null) {
            return NAMES[type];
        }
        if (type == 0x1F2) {
            return "ACAD_PROXY_ENTITY";
        }
        if (type == 0x1F3) {
            return "ACAD_PROXY_OBJECT";
        }
        if (type >= CLASS_BASE && classes != null) {
            DrawingClass dc = classes.get(type);
            if (dc != null && dc.classdxfname != null) {
                return dc.classdxfname;
            }
        }
        return String.format("UNKNOWN_0x%X", type);
    }

    public static boolean isEntity(int type, Map<Integer, DrawingClass> classes) {
        if (type >= CLASS_BASE) {
            DrawingClass dc = classes == null ? null : classes.get(type);
            return dc != null && dc.itemclassid == ITEM_CLASS_ENTITY;
        }
        return (type >= 0x01 && type <= 0x29)
            || (type >= 0x2B && type <= 0x2F)
            || type == 0x4A || type == 0x4D || type == 0x4E || type == 0x1F2;
    }
}
