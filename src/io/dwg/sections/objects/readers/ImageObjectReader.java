package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgImage;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/** IMAGE, spec §20.4.80; the fields are in the entity's values, see {@link RasterImageFields}. */
public class ImageObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.IMAGE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgImage e = (DwgImage) target;
        RasterImageFields.read(e, r, v);
        e.setInsertionPoint(RasterImageFields.point(e, "INSERTION"));
        e.setUVector(RasterImageFields.vector(e, "UVECTOR"));
        e.setVVector(RasterImageFields.vector(e, "VVECTOR"));
        io.dwg.core.type.Point3D size = RasterImageFields.point(e, "SIZE");
        e.setWidth(size.x());
        e.setHeight(size.y());
        e.setClippingState(Boolean.TRUE.equals(e.value("CLIPPING")) ? 1 : 0);
        e.setBrightness((Integer) e.value("BRIGHTNESS"));
        e.setContrast((Integer) e.value("CONTRAST"));
        e.setFade((Integer) e.value("FADE"));
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        RasterImageFields.readHandles((DwgImage) target, r);
    }
}
