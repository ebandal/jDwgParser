package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgInsert;
import io.dwg.sections.objects.ObjectReader;

public class InsertObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.INSERT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgInsert ins = (DwgInsert) target;
        double[] pt = r.read3BitDouble();
        ins.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        if (v.until(DwgVersion.R14)) {
            ins.setXScale(r.readBitDouble());
            ins.setYScale(r.readBitDouble());
            ins.setZScale(r.readBitDouble());
        } else {
            int scaleFlags = r.getInput().readBits(2);
            if (scaleFlags == 3) {
                ins.setXScale(1.0); ins.setYScale(1.0); ins.setZScale(1.0);
            } else if (scaleFlags == 1) {
                double s = r.getInput().readRawDouble();
                ins.setXScale(s); ins.setYScale(s); ins.setZScale(s);
            } else {
                ins.setXScale(r.readBitDouble());
                ins.setYScale(r.readBitDouble());
                ins.setZScale(r.readBitDouble());
            }
        }

        ins.setRotation(r.readBitDouble());
        ins.setExtrusion(r.readBitExtrusion());
        ins.setHasAttribs(r.getInput().readBit());

        // handle refs
        ins.setBlockHeaderHandle(new DwgHandleRef(r.readHandle()));
        if (ins.hasAttribs()) {
            ins.setFirstAttribHandle(new DwgHandleRef(r.readHandle()));
            ins.setLastAttribHandle(new DwgHandleRef(r.readHandle()));
            ins.setSeqendHandle(new DwgHandleRef(r.readHandle()));
        }
    }
}
