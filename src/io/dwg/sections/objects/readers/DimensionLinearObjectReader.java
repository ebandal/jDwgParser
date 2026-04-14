package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDimensionLinear;
import io.dwg.sections.objects.ObjectReader;

/**
 * DIMENSION_LINEAR 엔티티 리더 (타입 0x15)
 * 선형 크기 지정
 */
public class DimensionLinearObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_LINEAR.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDimensionLinear dim = (DwgDimensionLinear) target;

        // 1. definitionPoint (3RD)
        double[] pt = r.read3RawDouble();
        dim.setDefinitionPoint(new Point3D(pt[0], pt[1], pt[2]));

        // 2. midpointOfText (3RD)
        double[] mpt = r.read3RawDouble();
        dim.setMidpointOfText(new Point3D(mpt[0], mpt[1], mpt[2]));

        // 3. text (TU)
        String text = r.readVariableText();
        dim.setText(text);

        // 4. textRotation (BD)
        double textRotation = r.readBitDouble();
        dim.setTextRotation(textRotation);

        // 5. horizontalDirection (BD)
        double horizontalDirection = r.readBitDouble();
        dim.setHorizontalDirection(horizontalDirection);

        // 6. insertionScale (BD)
        double insertionScale = r.readBitDouble();
        dim.setInsertionScale(insertionScale);

        // 7. dimensionStyleName (TU)
        String dimensionStyleName = r.readVariableText();
        dim.setDimensionStyleName(dimensionStyleName);
    }
}
