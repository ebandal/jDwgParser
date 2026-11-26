package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgShape;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * SHAPE (type 33), spec §20.4.37: insertion point 3BD, scale BD, rotation BD, width factor BD,
 * oblique BD, thickness BD, shape number BS, extrusion 3BD; handle: the STYLE (shape file).
 * The width factor, oblique, thickness and shape number are in the entity's values (WIDTH,
 * OBLIQUE, THICKNESS, SHAPENO); the style handle is STYLE.
 */
public class ShapeObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SHAPE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgShape shape = (DwgShape) target;
        double[] p = r.read3BitDouble();
        shape.setInsertionPoint(new Point3D(p[0], p[1], p[2]));
        shape.setScale(r.readBitDouble());
        shape.setAngle(r.readBitDouble());
        Fields.read(shape, r, "WIDTH BD", "OBLIQUE BD", "THICKNESS BD", "SHAPENO BS");
        shape.setExtrusion(r.read3BitDouble());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgShape) target).setValue("STYLE", r.readRef(target.handle()));
    }
}
