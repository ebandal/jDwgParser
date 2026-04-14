package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTolerance;
import io.dwg.sections.objects.ObjectReader;

/**
 * TOLERANCE 엔티티 리더 (타입 0x2E)
 * 공차 기하공차 표시
 */
public class ToleranceObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.TOLERANCE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgTolerance tolerance = (DwgTolerance) target;

        // 1. insertionPoint (3RD)
        double[] pt = r.read3RawDouble();
        tolerance.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        // 2. dimensionStyleName (TU)
        String dimensionStyleName = r.readVariableText();
        tolerance.setDimensionStyleName(dimensionStyleName);

        // 3. toleranceText (TU)
        String toleranceText = r.readVariableText();
        tolerance.setToleranceText(toleranceText);

        // 4. direction (3RD)
        double[] dir = r.read3RawDouble();
        tolerance.setDirection(dir);
    }
}
