package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgGroup;
import io.dwg.sections.objects.ObjectReader;

/**
 * GROUP (type 72), spec §20.4.72: name TV, unnamed BS, selectable BS, number of members BL;
 * handles: (parent, reactors, xdictionary,) one per member.
 */
public class GroupObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.GROUP.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgGroup group = (DwgGroup) target;
        group.setGroupName(r.readVariableText());
        group.setUnnamed(r.readBitShort() != 0);
        group.setSelectable(r.readBitShort() != 0);
        group.setMemberCount(r.readBitLong());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgGroup group = (DwgGroup) target;
        for (int i = 0; i < group.memberCount(); i++) {
            group.addMember(r.readRef(target.handle()));
        }
    }
}
