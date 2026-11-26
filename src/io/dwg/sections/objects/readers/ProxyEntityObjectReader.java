package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgProxyEntity;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectHeaderData;
import io.dwg.sections.objects.ObjectReader;

/**
 * ACAD_PROXY_ENTITY, spec §20.4.90, after the common entity data:
 * <pre>
 *   R2000+ class id BL; before R2018 drawing format BL (version | maintenance version &lt;&lt; 16);
 *     R2018+ version BL, maintenance version BL; R2000+ original data format B (1: DXF)
 *   the entity's data bits, up to the end of the data (R2007+: and its string stream)
 *   handles: object handles, as many as there are up to the end of the object
 * </pre>
 */
public class ProxyEntityObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_PROXY_ENTITY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgProxyEntity proxy = (DwgProxyEntity) target;
        if (v.from(DwgVersion.R2000)) {
            proxy.setClassId(r.readBitLong());
            if (v.from(DwgVersion.R2018)) {
                proxy.setVersion(r.readBitLong());
                proxy.setMaintenanceVersion(r.readBitLong());
            } else {
                int format = r.readBitLong();
                proxy.setDrawingFormat(format);
                proxy.setVersion(format & 0xFFFF);
                proxy.setMaintenanceVersion(format >>> 16);
            }
            proxy.setDxfFormat(r.getInput().readBit());
        }
        long end = ObjectHeaderData.of(r).dataLimit(v.from(DwgVersion.R2007));
        if (end < r.position()) {
            throw new IllegalStateException("the end of the proxy data is not known");
        }
        long bits = end - r.position();
        proxy.setData(bits(r.getInput(), bits), bits);
        // R2007+: the strings of the entity's data are in the object's string stream
        ObjectHeaderData header = ObjectHeaderData.of(r);
        if (header.stringInput != null) {
            long stringBits = header.stringStreamEnd - header.stringInput.position();
            proxy.setStrings(bits(header.stringInput, stringBits), stringBits);
        }
    }

    private static byte[] bits(io.dwg.core.io.BitInput in, long bits) {
        byte[] data = new byte[(int) ((bits + 7) / 8)];
        for (long i = 0; i < bits; i++) {
            if (in.readBit()) {
                data[(int) (i / 8)] |= (byte) (0x80 >>> (i % 8));
            }
        }
        return data;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        long end = ObjectHeaderData.of(r).objectEnd;
        DwgProxyEntity proxy = (DwgProxyEntity) target;
        // a handle takes at least 8 bits; fewer left over are padding
        while (end - r.position() >= 8) {
            proxy.addObjectHandle(r.readRef(target.handle()));
        }
    }
}
