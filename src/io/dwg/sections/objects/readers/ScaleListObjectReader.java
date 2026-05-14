package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgScaleList;
import io.dwg.sections.objects.ObjectReader;

public class ScaleListObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_SCALE_LIST.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgScaleList list = (DwgScaleList) target;

        // Scale list name
        list.setListName(r.readText());

        // Is unit scale
        list.setUnitScale(r.readBitShort() != 0);

        // Scale paper units
        list.setScalePaperUnits(r.readBitShort());

        // Scale drawing units
        list.setScaleDrawingUnits(r.readBitShort());

        // Number of scale factors
        int numScales = r.readBitShort();
        for (int i = 0; i < numScales; i++) {
            list.scaleFactors().add(r.readBitDouble());
        }
    }
}
