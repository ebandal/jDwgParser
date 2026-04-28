package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgScale;
import io.dwg.sections.objects.ObjectReader;

public class ScaleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SCALE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgScale scale = (DwgScale) target;

        // Scale name
        scale.setScaleName(r.readText());

        // Paper units and drawing units
        scale.setPaperUnits(r.readBitDouble());
        scale.setDrawingUnits(r.readBitDouble());

        // Flags
        scale.setIsUnitScale(r.readBitShort() != 0);
        scale.setHasCustomScale(r.readBitShort() != 0);
    }
}
