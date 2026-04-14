package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.sections.objects.ObjectReader;

/**
 * BLOCK_HEADER 엔티티 리더 (타입 0x30)
 * 블록 정의의 시작
 */
public class BlockHeaderObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.BLOCK_HEADER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgBlockHeader block = (DwgBlockHeader) target;

        // 1. blockName (TU)
        String blockName = r.readVariableText();
        block.setBlockName(blockName);

        // 2. flags (BS)
        int flags = r.readBitShort();
        block.setFlags(flags);

        // 3. basePoint (3RD)
        double[] pt = r.read3RawDouble();
        block.setBasePoint(new Point3D(pt[0], pt[1], pt[2]));

        // 4. xrefPath (TU)
        String xrefPath = r.readVariableText();
        block.setXrefPath(xrefPath);
    }
}
