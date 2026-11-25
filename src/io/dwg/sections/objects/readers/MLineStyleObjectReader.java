package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLineStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * MLINESTYLE (type 73), spec §20.4.73.
 * <pre>
 *   name TV, description TV, flags BS, fill color CMC, start angle BD, end angle BD,
 *   number of lines RC, each: offset BD, color CMC, before R2018 linetype index BS
 *   handles: (parent, reactors, xdictionary,) R2018+ one linetype per line
 * </pre>
 * The flags are stored with some bits in other places than DXF group 70 has them; the table of
 * the specification maps them back.
 */
public class MLineStyleObjectReader implements ObjectReader {

    /** Pairs of bits: the DWG bit, then the DXF bit it stands for (spec §20.4.73). */
    private static final int[] FLAG_BITS = {
        1, 2, 2, 1, 16, 16, 32, 64, 64, 32, 256, 256, 512, 1024, 1024, 512,
    };

    @Override
    public int objectType() { return DwgObjectType.MLINESTYLE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMLineStyle style = (DwgMLineStyle) target;
        style.setName(r.readVariableText());
        style.setDescription(r.readVariableText());
        style.setFlags(toDxfFlags(r.readBitShort()));
        style.setFillColor(Colors.of(r.readCmColor()));
        style.setStartAngle(r.readBitDouble());
        style.setEndAngle(r.readBitDouble());
        int count = r.readRawChar();
        boolean indexed = !v.from(DwgVersion.R2018);
        for (int i = 0; i < count; i++) {
            double offset = r.readBitDouble();
            CmColor color = Colors.of(r.readCmColor());
            int linetypeIndex = indexed ? (short) r.readBitShort() : -1;
            style.addLine(new DwgMLineStyle.Line(offset, color, linetypeIndex, null));
        }
    }

    static int toDxfFlags(int dwgFlags) {
        int dxf = 0;
        for (int i = 0; i < FLAG_BITS.length; i += 2) {
            if ((dwgFlags & FLAG_BITS[i]) != 0) {
                dxf |= FLAG_BITS[i + 1];
            }
        }
        return dxf;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        if (!v.from(DwgVersion.R2018)) {
            return;
        }
        DwgMLineStyle style = (DwgMLineStyle) target;
        for (int i = 0; i < style.lines().size(); i++) {
            DwgHandleRef linetype = new DwgHandleRef(r.readHandleReference(target.handle()));
            style.setLine(i, style.lines().get(i).withLinetypeHandle(linetype));
        }
    }
}
