package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTolerance;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * TOLERANCE (type 46), spec §20.4.49: R13–R14 unknown BS, height BD, dimgap BD (in the values
 * HEIGHT, DIMGAP); insertion point 3BD, x direction 3BD, extrusion 3BD, text TV; handle: DIMSTYLE
 * (in the values). The specification gives the text as BS; it is a TV. The R13–R14 "unknown
 * short" is a BS (read as a raw short, the R13 and R14 samples fail; as a BS they end exactly).
 */
public class ToleranceObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.TOLERANCE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgTolerance tolerance = (DwgTolerance) target;
        if (!v.from(DwgVersion.R2000)) {
            r.readBitShort();                      // unknown
            Fields.read(tolerance, r, "HEIGHT BD", "DIMGAP BD");
        }
        double[] p = r.read3BitDouble();
        tolerance.setInsertionPoint(new Point3D(p[0], p[1], p[2]));
        tolerance.setDirection(r.read3BitDouble());
        Fields.read(tolerance, r, "EXTRUSION 3BD");
        tolerance.setToleranceText(r.readVariableText());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgTolerance) target).setValue("DIMSTYLE", r.readRef(target.handle()));
    }
}
