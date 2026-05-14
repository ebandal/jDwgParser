package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVisualStyle;
import io.dwg.sections.objects.ObjectReader;

public class VisualStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VISUALSTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVisualStyle style = (DwgVisualStyle) target;

        // Style name and description
        style.setStyleName(r.readText());
        style.setDescription(r.readText());

        // Style type
        style.setStyleType(r.readBitShort());

        // Opacity values
        style.setFaceOpacity(r.readBitDouble());
        style.setEdgeOpacity(r.readBitDouble());

        // Color modes
        style.setFaceColorMode(r.readBitShort());
        style.setEdgeColorMode(r.readBitShort());

        // Display options
        style.setIsDisplayEdges(r.readBitShort() != 0);
        style.setIsShowSilhouettes(r.readBitShort() != 0);

        // Silhouette width
        style.setSilhouetteWidth(r.readBitShort());
    }
}
