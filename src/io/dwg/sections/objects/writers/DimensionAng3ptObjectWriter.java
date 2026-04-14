package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDimensionAng3pt;
import io.dwg.sections.objects.ObjectWriter;

public class DimensionAng3ptObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ANG_3PT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgDimensionAng3pt dim = (DwgDimensionAng3pt) source;
        Point3D pt = dim.definitionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        Point3D mpt = dim.midpointOfText();
        w.write3RawDouble(new double[]{mpt.x(), mpt.y(), mpt.z()});
        w.writeVariableText(dim.text());
        w.writeBitDouble(dim.textRotation());
        w.writeBitDouble(dim.insertionScale());
        w.writeVariableText(dim.dimensionStyleName());
    }
}
