package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgHatch;
import io.dwg.sections.objects.ObjectWriter;

public class HatchObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.HATCH.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgHatch hatch = (DwgHatch) source;
        w.writeVariableText(hatch.patternName());
        w.writeBitShort(hatch.hatchStyle());
        w.writeBitDouble(hatch.scale());
        w.writeBitDouble(hatch.angle());
        w.writeBitLong(hatch.numBoundaryPaths());
    }
}
