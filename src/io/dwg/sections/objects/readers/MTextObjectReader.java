package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * MTEXT (type 44), spec §20.4.46.
 * <pre>
 *   insertion point 3BD, extrusion 3BD, x-axis direction 3BD, rectangle width BD,
 *   R2007+ rectangle height BD, text height BD, attachment BS, drawing direction BS,
 *   extents height BD, extents width BD, text TV,
 *   R2000+ line spacing style BS, line spacing factor BD, unknown B
 *   R2004+ background flags BL; with flag 1 (R2018+ also 0x10): background scale BD,
 *          background color CMC, background transparency BL
 *   R2018+ not annotative B; if not annotative: version BS, default flag B, attachment BL,
 *          x-axis direction 3BD, insertion point 3BD, rectangle width BD, rectangle height BD,
 *          extents width BD, extents height BD, column type BS; with columns: column count BL,
 *          column width BD, gutter BD, auto height B, flow reversed B, and for dynamic columns
 *          without auto height one height BD per column
 *   handles: STYLE, R2018+ not annotative: the registered application
 * </pre>
 * The specification gives the background scale as BL; it is a BD. Read as BL, each of the 81
 * MTEXTs of the samples that have a background ends 24 bits before its data does; read as BD,
 * all end exactly.
 */
public class MTextObjectReader implements ObjectReader {

    private static final int BACKGROUND_FILL = 0x01;
    private static final int TEXT_FRAME = 0x10;
    private static final int DYNAMIC_COLUMNS = 2;

    @Override
    public int objectType() { return DwgObjectType.MTEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgMText mtext = (DwgMText) target;

        mtext.setLocation(point(r.read3BitDouble()));
        mtext.setExtrusion(r.read3BitDouble());
        double[] xAxis = r.read3BitDouble();
        mtext.setXAxisDirection(point(xAxis));
        mtext.setAngle(Math.atan2(xAxis[1], xAxis[0]));

        mtext.setWidth(r.readBitDouble());
        if (v.from(DwgVersion.R2007)) {
            mtext.setRectangleHeight(r.readBitDouble());
        }
        mtext.setHeight(r.readBitDouble());
        mtext.setAttachmentPoint(r.readBitShort());
        mtext.setDrawingDirection(r.readBitShort());
        r.readBitDouble();                               // extents height
        r.readBitDouble();                               // extents width
        mtext.setText(r.readVariableText());

        if (v.from(DwgVersion.R2000)) {
            mtext.setLineSpacingStyle(r.readBitShort());
            mtext.setLineSpacingFactor(r.readBitDouble());
            r.getInput().readBit();                      // unknown bit
        }
        if (v.from(DwgVersion.R2004)) {
            int flags = r.readBitLong();
            mtext.setBackgroundFlags(flags);
            boolean background = (flags & BACKGROUND_FILL) != 0
                    || (v.from(DwgVersion.R2018) && (flags & TEXT_FRAME) != 0);
            if (background) {
                mtext.setBackgroundScale(r.readBitDouble());
                mtext.setBackgroundColor(Colors.of(r.readCmColor()));
                mtext.setBackgroundTransparency(r.readBitLong());
            }
        }
        if (v.from(DwgVersion.R2018)) {
            boolean notAnnotative = r.getInput().readBit();
            mtext.setNotAnnotative(notAnnotative);
            if (notAnnotative) {
                readRedundantFields(mtext, r);
            }
        }
    }

    private static void readRedundantFields(DwgMText mtext, BitStreamReader r) {
        r.readBitShort();                                // version
        r.getInput().readBit();                          // default flag
        r.readBitLong();                                 // attachment point
        r.read3BitDouble();                              // x-axis direction
        r.read3BitDouble();                              // insertion point
        r.readBitDouble();                               // rectangle width
        r.readBitDouble();                               // rectangle height
        r.readBitDouble();                               // extents width
        r.readBitDouble();                               // extents height
        int columnType = r.readBitShort();
        mtext.setColumnType(columnType);
        if (columnType != 0) {
            int count = r.readBitLong();
            mtext.setColumnCount(count);
            mtext.setColumnWidth(r.readBitDouble());
            mtext.setColumnGutter(r.readBitDouble());
            boolean autoHeight = r.getInput().readBit();
            mtext.setColumnAutoHeight(autoHeight);
            mtext.setColumnFlowReversed(r.getInput().readBit());
            if (!autoHeight && columnType == DYNAMIC_COLUMNS) {
                for (int i = 0; i < count; i++) {
                    mtext.addColumnHeight(r.readBitDouble());
                }
            }
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMText mtext = (DwgMText) target;
        mtext.setStyleHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
        if (v.from(DwgVersion.R2018) && mtext.isNotAnnotative()) {
            r.readHandleReference(target.handle());     // registered application
        }
    }

    private static Point3D point(double[] p) {
        return new Point3D(p[0], p[1], p[2]);
    }
}
