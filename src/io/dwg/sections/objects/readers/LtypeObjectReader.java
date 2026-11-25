package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * LTYPE (type 57), spec §20.4.58.
 * <pre>
 *   name TV, table record flags, description TV, pattern length BD, alignment RC,
 *   number of dashes RC, each: length BD, shape code BS, x offset RD, y offset RD, scale BD,
 *   rotation BD, shape flag BS
 *   R2004 and earlier: text area, 256 bytes
 *   R2007+: text area, 512 bytes, only if a dash has text
 *   handles: (control, reactors, xdictionary,) xref block, one style per dash
 * </pre>
 * The shape flag bits are 1 upright, 2 text, 4 shape. The specification gives 2 as shape and 4
 * as text, but the samples show the other way: the dashes with flag 2 have their text in the
 * text area, the one with flag 4 has a shape number. With that, the R2007+ rule of the
 * specification (a text area only when a dash has flag 2) means: only when a dash has text; the
 * one such sample ends at its data only when the area is read.
 * The text of a dash starts at the shape code: in R2007+ a character index into the UTF-16 area
 * (checked with the sample), before R2007 a byte offset into the area in the drawing's code page
 * (no sample has text).
 */
public class LtypeObjectReader implements ObjectReader {

    private static final int OLD_TEXT_AREA = 256;
    private static final int UNICODE_TEXT_AREA = 512;

    @Override
    public int objectType() { return DwgObjectType.LTYPE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLtype ltype = (DwgLtype) target;
        EntityHeaderReader.readTableRecordName(r, v, ltype);
        ltype.setDescription(r.readVariableText());
        ltype.setPatternLength(r.readBitDouble());
        ltype.setAlignment(r.readRawChar());

        int count = r.readRawChar();
        double[][] values = new double[count][];
        int[] shapeCodes = new int[count];
        int[] shapeFlags = new int[count];
        boolean hasText = false;
        for (int i = 0; i < count; i++) {
            double length = r.readBitDouble();
            shapeCodes[i] = r.readBitShort();
            double x = r.readRawDouble();
            double y = r.readRawDouble();
            double scale = r.readBitDouble();
            double rotation = r.readBitDouble();
            shapeFlags[i] = r.readBitShort();
            values[i] = new double[] {length, x, y, scale, rotation};
            hasText |= (shapeFlags[i] & DwgLtype.SHAPE_IS_TEXT) != 0;
        }

        boolean unicode = v.from(DwgVersion.R2007);
        byte[] area = null;
        if (!unicode || hasText) {
            area = new byte[unicode ? UNICODE_TEXT_AREA : OLD_TEXT_AREA];
            for (int i = 0; i < area.length; i++) {
                area[i] = (byte) r.readRawChar();
            }
        }
        ltype.setStringArea(area);

        for (int i = 0; i < count; i++) {
            double[] d = values[i];
            String text = (shapeFlags[i] & DwgLtype.SHAPE_IS_TEXT) != 0 && area != null
                    ? textAt(area, unicode ? 2 * shapeCodes[i] : shapeCodes[i], unicode ? StandardCharsets.UTF_16LE : r.textCharset())
                    : null;
            ltype.addDash(new DwgLtype.Dash(d[0], shapeCodes[i], d[1], d[2], d[3], d[4], shapeFlags[i], text, null));
        }
    }

    /** The 0-terminated string at {@code offset} of the text area. */
    private static String textAt(byte[] area, int offset, Charset charset) {
        if (offset < 0 || offset >= area.length) {
            return null;
        }
        int step = charset == StandardCharsets.UTF_16LE ? 2 : 1;
        int end = offset;
        while (end + step <= area.length && (area[end] != 0 || (step == 2 && area[end + 1] != 0))) {
            end += step;
        }
        return new String(area, offset, end - offset, charset);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLtype ltype = (DwgLtype) target;
        long self = target.handle();
        ltype.setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(self)));
        for (int i = 0; i < ltype.numDashes(); i++) {
            ltype.setDash(i, ltype.dashes().get(i).withStyleHandle(new DwgHandleRef(r.readHandleReference(self))));
        }
    }
}
