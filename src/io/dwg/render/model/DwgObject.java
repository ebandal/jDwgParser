package io.dwg.render.model;

/** Common header of one object found through the object map. */
public class DwgObject {
    public long handle;         // handle from the object map
    public int offset;          // file address of the object (start of the MS size)
    public int size;            // size of the object data in bytes (without MS size and CRC)
    public int dataOffset;      // file address of the object data (after the MS size)
    public int type;            // object type
    public String typeName;
    public boolean entity;      // true: graphical entity, false: non-graphical object
    public int bitSize;         // R2000+: size of the object data in bits (start of the handle stream)
    public long bodyBitPos;     // absolute bit position right after the object's own handle (start of EED)
}
