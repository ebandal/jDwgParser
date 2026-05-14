package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.sections.objects.ObjectReader;

public class LayerObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LAYER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayer layer = (DwgLayer) target;
        layer.setName(r.readVariableText());

        int flags = r.readBitShort();
        layer.setFlags(flags);
        layer.setFrozen((flags & 0x01) != 0);
        layer.setOn((flags & 0x02) == 0);
        layer.setFrozenInNewViewports((flags & 0x04) != 0);
        layer.setLocked((flags & 0x10) != 0);

        int[] colorData = r.readCmColor();
        layer.setColor(new CmColor(colorData[0]));

        // handle refs
        layer.setLineTypeHandle(new DwgHandleRef(r.readHandle()));
        if (v.from(DwgVersion.R2000)) {
            layer.setPlotStyleHandle(new DwgHandleRef(r.readHandle()));
        }
    }
}
