package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgWipeout;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/** WIPEOUT, spec §20.4.80 (WIPEOUT has the layout of IMAGE); the fields are in the entity's values, see {@link RasterImageFields}. */
public class WipeoutObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.WIPEOUT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgWipeout e = (DwgWipeout) target;
        RasterImageFields.read(e, r, v);
        e.setInsertionPoint(RasterImageFields.point(e, "INSERTION"));
        e.setUVector(RasterImageFields.vector(e, "UVECTOR"));
        e.setVVector(RasterImageFields.vector(e, "VVECTOR"));
        io.dwg.core.type.Point3D size = RasterImageFields.point(e, "SIZE");
        e.setWidth(size.x());
        e.setHeight(size.y());
        e.setClippingState(Boolean.TRUE.equals(e.value("CLIPPING")) ? 1 : 0);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        RasterImageFields.readHandles((DwgWipeout) target, r);
    }
}
