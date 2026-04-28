package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgImage;
import io.dwg.sections.objects.ObjectReader;

public class ImageObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.IMAGE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgImage image = (DwgImage) target;

        // Insertion point (3D vector)
        double[] point = r.read3BitDouble();
        image.setInsertionPoint(new Point3D(point[0], point[1], point[2]));

        // U vector (direction along width)
        double[] uVec = r.read3BitDouble();
        image.setUVector(uVec);

        // V vector (direction along height)
        double[] vVec = r.read3BitDouble();
        image.setVVector(vVec);

        // Width and height in image coordinates
        image.setWidth(r.readBitDouble());
        image.setHeight(r.readBitDouble());

        // Clipping state
        image.setClippingState(r.readBitShort());

        // Brightness, contrast, fade (ranges 0.0-1.0)
        image.setBrightness(r.readBitDouble());
        image.setContrast(r.readBitDouble());
        image.setFade(r.readBitDouble());
    }
}
