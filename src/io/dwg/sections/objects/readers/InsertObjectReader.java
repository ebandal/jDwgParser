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

/**
 * INSERT (type 7), spec §20.4.9; {@link MinsertObjectReader} adds the grid of MINSERT.
 * <pre>
 *   insertion point 3BD
 *   R13–R14 X, Y, Z scale BD
 *   R2000+  data flags BB: 11 scale (1, 1, 1); 01 X is 1.0, Y and Z DD with default 1.0;
 *           10 X RD, Y and Z equal to X; 00 X RD, Y and Z DD with X as default
 *   rotation BD, extrusion 3BD, has attribs B, R2004+ with attribs: owned object count BL
 *   handles: BLOCK_HEADER; with attribs: R13–R2000 first and last ATTRIB,
 *            R2004+ one ATTRIB per owned object; SEQEND
 * </pre>
 * The specification lists the owned object count for every R2004+ INSERT. The samples have it
 * only when the insert has attribs: read so, each of their 16,000 R2004+ INSERTs ends its data
 * and its handles exactly; read for every insert, those without attribs run 2 bits too far.
 */
public class InsertObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.INSERT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        readInsertFields((DwgInsert) target, r, v);
    }

    /** The fields of INSERT after the common entity data. */
    protected static void readInsertFields(DwgInsert ins, BitStreamReader r, DwgVersion v) {
        double[] pt = r.read3BitDouble();
        ins.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        if (!v.from(DwgVersion.R2000)) {
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
        if (v.from(DwgVersion.R2004) && ins.hasAttribs()) {
            ins.setOwnedObjectCount(r.readBitLong());
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgInsert ins = (DwgInsert) target;
        long self = target.handle();
        ins.setBlockHeaderHandle(r.readRef(self));
        if (!ins.hasAttribs()) {
            return;
        }
        if (v.from(DwgVersion.R2004)) {
            for (int i = 0; i < ins.ownedObjectCount(); i++) {
                ins.addAttribHandle(r.readRef(self));
            }
        } else {
            ins.setFirstAttribHandle(r.readRef(self));
            ins.setLastAttribHandle(r.readRef(self));
        }
        ins.setSeqendHandle(r.readRef(self));
    }
}
