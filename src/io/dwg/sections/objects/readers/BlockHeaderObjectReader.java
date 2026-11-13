package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * BLOCK_HEADER 테이블 레코드 리더 (타입 0x31)
 *
 * R2000 파일로 검증했다. R2004 이후의 필드는 스펙을 따랐지만 검증하지 않았다.
 */
public class BlockHeaderObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.BLOCK_HEADER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readObjectHeader(r, v);

        DwgBlockHeader block = (DwgBlockHeader) target;

        // 테이블 레코드 공통 : 이름, 64-flag, xref index + 1, xdep
        block.setBlockName(r.readVariableText());
        r.getInput().readBit();
        r.readBitShort();
        r.getInput().readBit();

        int flags = 0;
        if (r.getInput().readBit()) flags |= 0x01;     // anonymous
        if (r.getInput().readBit()) flags |= 0x02;     // has attributes
        if (r.getInput().readBit()) flags |= 0x04;     // is xref
        if (r.getInput().readBit()) flags |= 0x08;     // xref overlaid
        block.setFlags(flags);
        if (v.from(DwgVersion.R2000)) {
            r.getInput().readBit();                     // loaded
        }
        if (v.from(DwgVersion.R2004)) {
            r.readBitLong();                            // owned object count
        }

        double[] pt = r.read3BitDouble();
        block.setBasePoint(new Point3D(pt[0], pt[1], pt[2]));
        block.setXrefPath(r.readVariableText());
    }
}
