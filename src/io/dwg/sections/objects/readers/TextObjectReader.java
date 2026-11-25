package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgText;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * TEXT (type 1), spec §20.4.3. ATTRIB and ATTDEF start with the same fields.
 */
public class TextObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.TEXT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        readTextData((DwgText) target, r, v);
    }

    /** Reads the TEXT fields that follow the common entity data. */
    public static void readTextData(DwgText text, BitStreamReader r, DwgVersion v) throws Exception {
        if (v.from(DwgVersion.R2000)) {
            readR2000(text, r);
        } else {
            readR13(text, r);
        }
    }

    /**
     * R13–R14: elevation BD, insertion 2RD, alignment 2RD, extrusion 3BD, thickness BD,
     * oblique BD, rotation BD, height BD, width factor BD, value TV, generation BS,
     * horizontal and vertical alignment BS.
     */
    private static void readR13(DwgText text, BitStreamReader r) {
        text.setElevation(r.readBitDouble());
        text.setInsertionPoint(point(r.read2RawDouble()));
        text.setAlignmentPoint(point(r.read2RawDouble()));
        r.read3BitDouble();                              // extrusion, not kept for R13–R14
        text.setThickness(r.readBitDouble());
        text.setObliquAngle(r.readBitDouble());
        text.setRotationAngle(r.readBitDouble());
        text.setHeight(r.readBitDouble());
        text.setWidthFactor(r.readBitDouble());
        text.setValue(r.readVariableText());
        text.setGeneration(r.readBitShort());
        text.setHorizontalAlignment(r.readBitShort());
        text.setVerticalAlignment(r.readBitShort());
    }

    /**
     * R2000+: a data flags byte (RC) tells which fields are left out; a field is present when
     * its bit is 0. Most doubles are raw doubles; the alignment point is a 2DD with the insertion
     * point as default.
     */
    private static void readR2000(DwgText text, BitStreamReader r) {
        int flags = r.getInput().readBits(8);
        text.setDataFlags(flags);

        if (present(flags, 0x01)) text.setElevation(r.readRawDouble());
        double[] insertion = r.read2RawDouble();
        text.setInsertionPoint(point(insertion));
        if (present(flags, 0x02)) text.setAlignmentPoint(point(r.read2DD(insertion[0], insertion[1])));
        text.setExtrusion(r.readBitExtrusion());
        text.setThickness(r.readBitThickness());
        if (present(flags, 0x04)) text.setObliquAngle(r.readRawDouble());
        if (present(flags, 0x08)) text.setRotationAngle(r.readRawDouble());
        text.setHeight(r.readRawDouble());
        text.setWidthFactor(present(flags, 0x10) ? r.readRawDouble() : 1.0);
        text.setValue(r.readVariableText());
        if (present(flags, 0x20)) text.setGeneration(r.readBitShort());
        if (present(flags, 0x40)) text.setHorizontalAlignment(r.readBitShort());
        if (present(flags, 0x80)) text.setVerticalAlignment(r.readBitShort());
    }

    private static boolean present(int dataFlags, int bit) {
        return (dataFlags & bit) == 0;
    }

    private static Point2D point(double[] xy) {
        return new Point2D(xy[0], xy[1]);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgText) target).setStyleHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
