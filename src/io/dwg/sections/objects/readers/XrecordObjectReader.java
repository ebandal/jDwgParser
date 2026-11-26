package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgXrecord;
import io.dwg.sections.objects.ObjectReader;
import io.dwg.sections.objects.XrecordDataDecoder;

/**
 * XRECORD, spec §20.4.104.
 * <pre>
 *   number of data bytes BL, the data bytes, R2000+ cloning flag BS
 *   handles: (parent, reactors, xdictionary,) object handles up to the end of the object
 * </pre>
 * The data is decoded by {@link XrecordDataDecoder}.
 */
public class XrecordObjectReader implements ObjectReader {

    /** A handle reference takes at least one byte. */
    private static final int SMALLEST_HANDLE_BITS = 8;

    @Override
    public int objectType() { return DwgObjectType.XRECORD.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgXrecord xrecord = (DwgXrecord) target;
        int count = r.count(r.readBitLong(), 8);
        byte[] data = new byte[count];
        for (int i = 0; i < count; i++) {
            data[i] = (byte) r.readRawChar();
        }
        xrecord.setRecordData(data);
        XrecordDataDecoder.decode(data, v, r.textCharset()).forEach(xrecord::addItem);
        if (v.from(DwgVersion.R2000)) {
            xrecord.setDuplicateRecordCloning(r.readBitShort());
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        if (!(r.getInput() instanceof ByteBufferBitInput)) {
            return;
        }
        long end = ((ByteBufferBitInput) r.getInput()).limitBit();
        DwgXrecord xrecord = (DwgXrecord) target;
        while (r.position() + SMALLEST_HANDLE_BITS <= end) {
            xrecord.addObjectHandle(r.readRef(target.handle()));
        }
    }
}
