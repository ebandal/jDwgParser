package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDimensionAligned;
import io.dwg.sections.objects.ObjectReader;

/**
 * DIMENSION_ALIGNED 엔티티 리더 (타입 0x16)
 * 정렬된 크기 지정
 */
public class DimensionAlignedObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ALIGNED.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDimensionAligned dim = (DwgDimensionAligned) target;

        double[] pt = r.read3RawDouble();
        dim.setDefinitionPoint(new Point3D(pt[0], pt[1], pt[2]));

        double[] mpt = r.read3RawDouble();
        dim.setMidpointOfText(new Point3D(mpt[0], mpt[1], mpt[2]));

        String text = r.readVariableText();
        dim.setText(text);

        double textRotation = r.readBitDouble();
        dim.setTextRotation(textRotation);

        double insertionScale = r.readBitDouble();
        dim.setInsertionScale(insertionScale);

        String dimensionStyleName = r.readVariableText();
        dim.setDimensionStyleName(dimensionStyleName);
    }
}
