package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLeader;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * LEADER 엔티티 리더 (타입 0x2D)
 * 주석을 가리키는 선 (화살표 포함)
 */
public class LeaderObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LEADER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgLeader leader = (DwgLeader) target;
        boolean r13or14 = !v.from(DwgVersion.R2000);

        r.getInput().readBit();                 // unknown
        r.readBitShort();                       // annotation type
        leader.setPathType(r.readBitShort());

        int numPoints = r.readBitLong();
        if (numPoints < 0 || numPoints > 1_000_000) {
            throw new IllegalStateException("bad LEADER point count " + numPoints);
        }
        for (int i = 0; i < numPoints; i++) {
            double[] pt = r.read3BitDouble();
            leader.addPoint(new Point3D(pt[0], pt[1], pt[2]));
        }

        r.read3BitDouble();                     // origin
        leader.setExtrusion(r.read3BitDouble());
        r.read3BitDouble();                     // x direction
        r.read3BitDouble();                     // offset to the block insertion point
        if (v.from(DwgVersion.R14)) {
            r.read3BitDouble();                 // end point projection
        }
        if (r13or14) {
            r.readBitDouble();                  // dimgap
        }
        if (!v.from(DwgVersion.R2010)) {
            r.readBitDouble();                  // box height
            r.readBitDouble();                  // box width
        }
        r.getInput().readBit();                 // hook line on x direction
        leader.setArrowheadOn(r.getInput().readBit());
        leader.setArrow(leader.isArrowheadOn() ? 1 : 0);
        if (r13or14) {
            r.readBitShort();                   // arrowhead type
            r.readBitDouble();                  // dimasz
            r.getInput().readBit();
            r.getInput().readBit();
            r.readBitShort();
            r.readBitShort();                   // byblock color
            r.getInput().readBit();
            r.getInput().readBit();
        } else {
            r.readBitShort();                   // unknown
            r.getInput().readBit();             // unknown
            r.getInput().readBit();             // unknown
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        r.readHandleReference(target.handle()); // associated annotation
        r.readHandleReference(target.handle()); // dimension style
    }
}
