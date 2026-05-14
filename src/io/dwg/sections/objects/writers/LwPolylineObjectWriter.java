package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLwPolyline;
import io.dwg.sections.objects.ObjectWriter;
import java.util.List;

public class LwPolylineObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.LWPLINE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgLwPolyline lwp = (DwgLwPolyline) source;

        w.writeBitShort(lwp.flags());
        w.writeBitDouble(lwp.constantWidth());
        w.writeBitDouble(lwp.elevation());
        w.writeBitThickness(lwp.thickness());
        w.writeBitExtrusion(lwp.extrusion());

        List<Double> bulges = lwp.bulges();
        w.writeBitLong(bulges.size());

        List<double[]> widths = lwp.widths();
        w.writeBitLong(widths.size());

        List<Point2D> vertices = lwp.vertices();
        w.writeBitLong(vertices.size());

        for (Double b : bulges) {
            w.writeBitDouble(b);
        }

        for (double[] width : widths) {
            w.writeBitDouble(width[0]);
            w.writeBitDouble(width[1]);
        }

        for (Point2D vertex : vertices) {
            w.writeBitDouble(vertex.x());
            w.writeBitDouble(vertex.y());
        }
    }
}
