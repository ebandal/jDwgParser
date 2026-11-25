package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * MTEXT (type 44), spec §20.4.46. Reads the fields up to the R2000 line spacing; the background
 * and column fields that follow are not needed by the library.
 * <pre>
 *   insertion point 3BD, extrusion 3BD, x-axis direction 3BD, rectangle width BD,
 *   R2007+ rectangle height BD, text height BD, attachment BS, drawing direction BS,
 *   extents height BD, extents width BD, text TV,
 *   R2000+ line spacing style BS, line spacing factor BD, unknown B
 * </pre>
 */
public class MTextObjectReader implements ObjectReader {

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
            r.readBitDouble();                           // rectangle height
        }
        mtext.setHeight(r.readBitDouble());
        mtext.setAttachmentPoint(r.readBitShort());
        r.readBitShort();                                // drawing direction
        r.readBitDouble();                               // extents height
        r.readBitDouble();                               // extents width
        mtext.setText(r.readVariableText());

        if (v.from(DwgVersion.R2000)) {
            r.readBitShort();                            // line spacing style
            r.readBitDouble();                           // line spacing factor
            r.getInput().readBit();                      // unknown bit
        }
    }

    private static Point3D point(double[] p) {
        return new Point3D(p[0], p[1], p[2]);
    }
}
