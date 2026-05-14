package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPersSubentManager;
import io.dwg.sections.objects.ObjectReader;

public class PersSubentManagerObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_PERSSUBENTMANAGER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPersSubentManager manager = (DwgPersSubentManager) target;

        // Manager name
        manager.setManagerName(r.readText());

        // Subentity count
        int count = r.readBitShort();
        manager.setSubentityCount(count);

        // Read subentity IDs and names
        for (int i = 0; i < count && i < 1000; i++) {
            try {
                manager.subentityIds().add(r.readBitShort());
                manager.subentityNames().add(r.readText());
            } catch (Exception e) {
                break;
            }
        }

        // Manager flags
        manager.setManagerFlags(r.readBitShort());

        // Is active
        manager.setActive(r.readBitShort() != 0);
    }
}
