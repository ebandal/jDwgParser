package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPlotStyle;
import io.dwg.sections.objects.ObjectReader;

public class PlotStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_PLOTSTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPlotStyle style = (DwgPlotStyle) target;

        // Plot style name
        style.setPlotStyleName(r.readText());

        // Description
        style.setDescription(r.readText());

        // Line weight value
        style.setLineWeightValue(r.readBitShort());

        // Line type value
        style.setLineTypeValue(r.readBitShort());

        // Transparency value
        style.setTransparencyValue(r.readBitDouble());

        // Is adaptive
        style.setAdaptive(r.readBitShort() != 0);
    }
}
