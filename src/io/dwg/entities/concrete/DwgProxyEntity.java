package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ACAD_PROXY_ENTITY (spec §20.4.90): an entity of a class whose application was not present when
 * the drawing was saved. Its own data is not interpreted: it is kept as the bits that follow the
 * proxy fields ({@link #data()}), with the object handles it refers to. The whole object is also
 * in {@link #rawData()}.
 */
public class DwgProxyEntity extends AbstractDwgEntity {
    private int classId;
    private int drawingFormat;
    private int version;
    private int maintenanceVersion;
    private boolean dxfFormat;
    private byte[] data = new byte[0];
    private long dataBits;
    private byte[] strings = new byte[0];
    private long stringBits;
    private final List<DwgHandleRef> objectHandles = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_PROXY_ENTITY; }

    /** R2000+: the class number (DXF 91). */
    public int classId() { return classId; }
    /** R2000 to R2013: the version OR the maintenance version shifted left 16 bits (DXF 95). */
    public int drawingFormat() { return drawingFormat; }
    /** The AutoCAD version of the object (DXF 71); before R2018 taken from {@link #drawingFormat()}. */
    public int version() { return version; }
    /** The AutoCAD maintenance version of the object (DXF 97); before R2018 taken from {@link #drawingFormat()}. */
    public int maintenanceVersion() { return maintenanceVersion; }
    /** R2000+: the original data format is DXF (DXF 70 = 1) rather than DWG. */
    public boolean isDxfFormat() { return dxfFormat; }
    /** The entity's own data bits, packed from the most significant bit of the first byte. */
    public byte[] data() { return data.clone(); }
    /** The number of bits in {@link #data()}. */
    public long dataBits() { return dataBits; }
    /** R2007+: the string stream of the entity's data (where its strings are), packed like {@link #data()}. */
    public byte[] strings() { return strings.clone(); }
    /** The number of bits in {@link #strings()}. */
    public long stringBits() { return stringBits; }
    /** The object handles after the common handles. */
    public List<DwgHandleRef> objectHandles() { return Collections.unmodifiableList(objectHandles); }

    public void setClassId(int classId) { this.classId = classId; }
    public void setDrawingFormat(int format) { this.drawingFormat = format; }
    public void setVersion(int version) { this.version = version; }
    public void setMaintenanceVersion(int version) { this.maintenanceVersion = version; }
    public void setDxfFormat(boolean dxf) { this.dxfFormat = dxf; }
    public void setData(byte[] data, long bits) { this.data = data.clone(); this.dataBits = bits; }
    public void setStrings(byte[] strings, long bits) { this.strings = strings.clone(); this.stringBits = bits; }
    public void addObjectHandle(DwgHandleRef h) { objectHandles.add(h); }
}
