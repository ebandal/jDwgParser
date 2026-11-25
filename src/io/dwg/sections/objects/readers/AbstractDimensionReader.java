package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObject;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * The seven DIMENSION types: the common entity data, the common dimension data of spec §20.4.22,
 * then the fields of the type ({@link #readTypeFields}).
 * <pre>
 *   R2010+  version RC
 *   extrusion 3BD, text midpoint 2RD, elevation BD, flags 1 RC, user text TV,
 *   text rotation BD, horizontal direction BD, insertion scale X, Y, Z BD, insertion rotation BD
 *   R2000+  attachment point BS, line spacing style BS, line spacing factor BD,
 *           actual measurement BD
 *   R2007+  unknown B, flip arrow 1 B, flip arrow 2 B
 *   12 point 2RD
 *   handles: DIMSTYLE, anonymous block
 * </pre>
 * The 2D points (11, 12, 16) take the elevation as Z.
 */
abstract class AbstractDimensionReader implements ObjectReader {

    @Override
    public final void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        AbstractDwgDimension dim = (AbstractDwgDimension) target;

        if (v.from(DwgVersion.R2010)) {
            dim.setDimensionVersion(r.readRawChar());
        }
        dim.setExtrusion(r.read3BitDouble());
        double[] textMid = r.read2RawDouble();
        double elevation = r.readBitDouble();
        dim.setTextMidpoint(new Point3D(textMid[0], textMid[1], elevation));
        dim.setFlags1(r.readRawChar());
        dim.setText(r.readVariableText());
        dim.setTextRotation(r.readBitDouble());
        dim.setHorizontalDirection(r.readBitDouble());
        dim.setInsertionScales(new Point3D(r.readBitDouble(), r.readBitDouble(), r.readBitDouble()));
        dim.setInsertionRotation(r.readBitDouble());
        if (v.from(DwgVersion.R2000)) {
            dim.setAttachmentPoint(r.readBitShort());
            dim.setLineSpacingStyle(r.readBitShort());
            dim.setLineSpacingFactor(r.readBitDouble());
            dim.setActualMeasurement(r.readBitDouble());
        }
        if (v.from(DwgVersion.R2007)) {
            r.getInput().readBit();                // unknown (DXF 73)
            dim.setFlipArrow1(r.getInput().readBit());
            dim.setFlipArrow2(r.getInput().readBit());
        }
        double[] p12 = r.read2RawDouble();
        dim.setPoint12(new Point3D(p12[0], p12[1], elevation));

        readTypeFields(dim, r, v, elevation);
    }

    /** The fields of the dimension type, after the common dimension data. */
    protected abstract void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation);

    protected static Point3D point(BitStreamReader r) {
        double[] p = r.read3BitDouble();
        return new Point3D(p[0], p[1], p[2]);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgDimension dim = (AbstractDwgDimension) target;
        dim.setDimStyleHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
        dim.setBlockHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
