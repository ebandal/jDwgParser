package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgInsert;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

public class InsertObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.INSERT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

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
            } else if (scaleFlags == 2) {
                double s = r.readRawDouble();
                ins.setXScale(s); ins.setYScale(s); ins.setZScale(s);
            } else if (scaleFlags == 1) {
                ins.setXScale(1.0);
                ins.setYScale(r.readDD(1.0));
                ins.setZScale(r.readDD(1.0));
            } else {
                double x = r.readRawDouble();
                ins.setXScale(x);
                ins.setYScale(r.readDD(x));
                ins.setZScale(r.readDD(x));
            }
        }

        ins.setRotation(r.readBitDouble());
        ins.setExtrusion(r.read3BitDouble());
        ins.setHasAttribs(r.getInput().readBit());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgInsert ins = (DwgInsert) target;
        long self = target.handle();
        ins.setBlockHeaderHandle(new DwgHandleRef(r.readHandleReference(self)));
        // R2004+ lists every attribute instead of the first and the last; that list is not read here
        if (ins.hasAttribs() && !v.from(DwgVersion.R2004)) {
            ins.setFirstAttribHandle(new DwgHandleRef(r.readHandleReference(self)));
            ins.setLastAttribHandle(new DwgHandleRef(r.readHandleReference(self)));
            ins.setSeqendHandle(new DwgHandleRef(r.readHandleReference(self)));
        }
    }
}
