package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgUnderlay;
import io.dwg.sections.objects.ObjectReader;

public class UnderlayObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.UNDERLAY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgUnderlay underlay = (DwgUnderlay) target;

        // Insertion point
        double[] point = r.read3BitDouble();
        underlay.setInsertionPoint(new Point3D(point[0], point[1], point[2]));

        // Scale factors
        double scaleX = r.readBitDouble();
        double scaleY = r.readBitDouble();
        double scaleZ = r.readBitDouble();
        underlay.setScale(new double[]{scaleX, scaleY, scaleZ});

        // Rotation angle
        underlay.setRotation(r.readBitDouble());

        // Underlay type (0=PDF, 1=DWF, 2=DGN)
        underlay.setUnderlayType(r.readBitShort());

        // Path to underlay file
        underlay.setUnderlayPath(r.readText());

        // Opacity (0.0-1.0)
        underlay.setOpacity(r.readBitDouble());

        // Clipping and boundary
        underlay.setIsClipped(r.readBitShort() != 0);
        underlay.setClipBoundaryType(r.readBitShort());
    }
}
