package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgGroup;
import io.dwg.sections.objects.ObjectReader;

/**
 * GROUP 엔티티 리더 (타입 0x3C)
 * 여러 엔티티의 그룹화
 */
public class GroupObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.GROUP.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgGroup group = (DwgGroup) target;

        // 1. groupName (TU)
        String groupName = r.readVariableText();
        group.setGroupName(groupName);

        // 2. isUnnamed (B - 1 bit)
        boolean isUnnamed = r.getInput().readBit();
        group.setUnnamed(isUnnamed);

        // 3. isSelectable (B)
        boolean isSelectable = r.getInput().readBit();
        group.setSelectable(isSelectable);

        // 4. numMembers (BL)
        long numMembers = r.readBitLong();

        // 5. members - numMembers개의 핸들
        for (int i = 0; i < numMembers; i++) {
            long memberHandle = r.readBitLongLong();
            group.addMember(memberHandle);
        }
    }
}
