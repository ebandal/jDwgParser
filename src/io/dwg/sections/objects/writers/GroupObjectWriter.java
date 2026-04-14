package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgGroup;
import io.dwg.sections.objects.ObjectWriter;

public class GroupObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.GROUP.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgGroup group = (DwgGroup) source;
        w.writeVariableText(group.groupName());
        w.getOutput().writeBit(group.isUnnamed());
        w.getOutput().writeBit(group.isSelectable());
        w.writeBitLong(group.members().size());
        for (Object member : group.members()) {
            w.writeBitLongLong((Long) member);
        }
    }
}
