package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The fields of IMAGE (spec §20.4.80), which WIPEOUT shares, kept in the entity's values:
 * <pre>
 *   CLASSVERSION BL, INSERTION, UVECTOR, VVECTOR 3BD, SIZE 2RD, DISPLAYPROPS BS, CLIPPING B,
 *   BRIGHTNESS, CONTRAST, FADE RC, R2010+ CLIPMODE B, CLIPBOUNDARYTYPE BS, then CLIPBOUNDARY:
 *   type 1 two corners 2RD, otherwise a BL count and that many 2RD
 *   handles: IMAGEDEF, IMAGEDEFREACTOR
 * </pre>
 */
final class RasterImageFields {

    private static final int RECTANGLE = 1;

    private RasterImageFields() {
    }

    static void read(AbstractDwgEntity e, BitStreamReader r, DwgVersion v) {
        Fields.read(e, r, "CLASSVERSION BL", "INSERTION 3BD", "UVECTOR 3BD", "VVECTOR 3BD", "SIZE 2RD",
                "DISPLAYPROPS BS", "CLIPPING B", "BRIGHTNESS RC", "CONTRAST RC", "FADE RC");
        if (v.from(DwgVersion.R2010)) {
            Fields.read(e, r, "CLIPMODE B");
        }
        int type = r.readBitShort();
        e.setValue("CLIPBOUNDARYTYPE", type);
        int count = type == RECTANGLE ? 2 : r.readBitLong();
        List<Point3D> boundary = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] p = r.read2RawDouble();
            boundary.add(new Point3D(p[0], p[1], 0));
        }
        e.setValue("CLIPBOUNDARY", boundary);
    }

    static void readHandles(AbstractDwgEntity e, BitStreamReader r) {
        e.setValue("IMAGEDEF", r.readRef(e.handle()));
        e.setValue("IMAGEDEFREACTOR", r.readRef(e.handle()));
    }

    static Point3D point(AbstractDwgEntity e, String name) {
        Object o = e.value(name);
        return o instanceof Point3D ? (Point3D) o : null;
    }

    static double[] vector(AbstractDwgEntity e, String name) {
        Point3D p = point(e, name);
        return p == null ? null : new double[] {p.x(), p.y(), p.z()};
    }
}
