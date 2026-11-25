package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgScale;
import io.dwg.sections.objects.ObjectReader;

/**
 * SCALE (a class), spec §20.4.92: unknown BS, name TV, paper units BD, drawing units BD, has unit
 * scale B.
 */
public class ScaleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SCALE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgScale scale = (DwgScale) target;
        r.readBitShort();                          // unknown, 0
        scale.setScaleName(r.readVariableText());
        scale.setPaperUnits(r.readBitDouble());
        scale.setDrawingUnits(r.readBitDouble());
        scale.setIsUnitScale(r.getInput().readBit());
    }
}
