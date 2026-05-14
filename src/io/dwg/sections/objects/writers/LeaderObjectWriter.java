package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLeader;
import io.dwg.sections.objects.ObjectWriter;
import java.util.List;

public class LeaderObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.LEADER.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgLeader leader = (DwgLeader) source;
        w.writeVariableText(leader.styleName());
        w.writeBitShort(leader.arrow());

        List<Point3D> points = leader.points();
        w.writeBitShort(points.size());
        for (Point3D pt : points) {
            w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        }

        w.writeBitExtrusion(leader.extrusion());
    }
}
