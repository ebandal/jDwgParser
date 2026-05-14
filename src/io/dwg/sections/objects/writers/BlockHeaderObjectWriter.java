package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.sections.objects.ObjectWriter;

public class BlockHeaderObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.BLOCK_HEADER.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgBlockHeader block = (DwgBlockHeader) source;
        w.writeVariableText(block.blockName());
        w.writeBitShort(block.flags());
        Point3D pt = block.basePoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.writeVariableText(block.xrefPath());
    }
}
