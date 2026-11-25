package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * BLOCK_HEADER (type 49), spec §20.4.52.
 * <pre>
 *   name TV, table record flags, anonymous B, has attdefs B, is xref B, overlaid xref B,
 *   R2000+ loaded B (0 means loaded), R2004+ owned object count BL,
 *   base point 3BD, xref path TV,
 *   R2000+ insert count: non-zero RCs ended by a 0 RC, description TV,
 *          preview data size BL and that many bytes,
 *   R2007+ insert units BS, explodable B, block scaling RC
 *   handles: (control, reactors, xdictionary,) xref block (null), BLOCK entity,
 *     R13–R2000 unless xref or overlaid: first and last entity,
 *     R2004+ the owned entities, ENDBLK, R2000+ the inserts and the layout
 * </pre>
 */
public class BlockHeaderObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.BLOCK_HEADER.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgBlockHeader block = (DwgBlockHeader) target;
        EntityHeaderReader.readTableRecordName(r, v, block);

        int flags = 0;
        if (r.getInput().readBit()) flags |= 0x01;
        if (r.getInput().readBit()) flags |= 0x02;
        if (r.getInput().readBit()) flags |= 0x04;
        if (r.getInput().readBit()) flags |= 0x08;
        block.setFlags(flags);
        if (v.from(DwgVersion.R2000)) {
            block.setLoaded(!r.getInput().readBit());
        }
        if (v.from(DwgVersion.R2004)) {
            block.setOwnedObjectCount(r.readBitLong());
        }
        double[] base = r.read3BitDouble();
        block.setBasePoint(new Point3D(base[0], base[1], base[2]));
        block.setXrefPath(r.readVariableText());

        if (v.from(DwgVersion.R2000)) {
            int inserts = 0;
            while (r.readRawChar() != 0) {
                inserts++;
            }
            block.setInsertCount(inserts);
            block.setDescription(r.readVariableText());
            int size = r.readBitLong();
            if (size > 0) {
                byte[] preview = new byte[size];
                for (int i = 0; i < size; i++) {
                    preview[i] = (byte) r.readRawChar();
                }
                block.setPreviewData(preview);
            }
        }
        if (v.from(DwgVersion.R2007)) {
            block.setInsertUnits(r.readBitShort());
            block.setExplodable(r.getInput().readBit());
            block.setBlockScaling(r.readRawChar());
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgBlockHeader block = (DwgBlockHeader) target;
        long self = target.handle();
        block.setXrefBlockHandle(handle(r, self));
        block.setBlockEntityHandle(handle(r, self));
        if (!v.from(DwgVersion.R2004) && !block.isXref() && !block.isXrefOverlay()) {
            block.setFirstEntityHandle(handle(r, self));
            block.setLastEntityHandle(handle(r, self));
        }
        if (v.from(DwgVersion.R2004)) {
            for (int i = 0; i < block.ownedObjectCount(); i++) {
                block.addOwnedEntityHandle(handle(r, self));
            }
        }
        block.setEndBlockHandle(handle(r, self));
        if (v.from(DwgVersion.R2000)) {
            for (int i = 0; i < block.insertCount(); i++) {
                block.addInsertHandle(handle(r, self));
            }
            block.setLayoutHandle(handle(r, self));
        }
    }

    private static DwgHandleRef handle(BitStreamReader r, long self) {
        return new DwgHandleRef(r.readHandleReference(self));
    }
}
