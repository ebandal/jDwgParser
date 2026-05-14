package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgXref;
import io.dwg.sections.objects.ObjectReader;

public class XrefObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.XREF.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgXref xref = (DwgXref) target;

        // Insertion point
        double[] point = r.read3BitDouble();
        xref.setInsertionPoint(new Point3D(point[0], point[1], point[2]));

        // Scale factors
        double scaleX = r.readBitDouble();
        double scaleY = r.readBitDouble();
        double scaleZ = r.readBitDouble();
        xref.setScale(new double[]{scaleX, scaleY, scaleZ});

        // Rotation angle
        xref.setRotation(r.readBitDouble());

        // Xref type (0=attached, 1=overlaid)
        xref.setXrefType(r.readBitShort());

        // External reference path
        xref.setReferencePath(r.readText());
    }
}
