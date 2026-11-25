package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * LAYER (type 51), spec §20.4.54.
 * <pre>
 *   name TV, table record flags,
 *   R13–R14 frozen B, off B, frozen in new viewports B, locked B
 *   R2000+  flags BS: 1 frozen, 2 off, 4 frozen in new viewports, 8 locked, 16 plotted,
 *           0x03E0 lineweight
 *   color CMC
 *   handles: (control, reactors, xdictionary,) xref block, R2000+ plot style, R2007+ material,
 *            linetype, R2013+ one more handle (null)
 * </pre>
 */
public class LayerObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LAYER.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayer layer = (DwgLayer) target;
        EntityHeaderReader.readTableRecordName(r, v, layer);

        if (v.from(DwgVersion.R2000)) {
            int flags = r.readBitShort();
            layer.setFlags(flags);
            layer.setFrozen((flags & 0x01) != 0);
            layer.setOn((flags & 0x02) == 0);
            layer.setFrozenInNewViewports((flags & 0x04) != 0);
            layer.setLocked((flags & 0x08) != 0);
            layer.setPlotted((flags & 0x10) != 0);
            layer.setLineWeight((flags & 0x03E0) >> 5);
        } else {
            // R13/R14 : frozen, off, frozen in new viewports, locked (B each).
            // The second bit is set when the layer is switched off (checked against the R2000 files).
            layer.setFrozen(r.getInput().readBit());
            layer.setOn(!r.getInput().readBit());
            layer.setFrozenInNewViewports(r.getInput().readBit());
            layer.setLocked(r.getInput().readBit());
        }

        layer.setColor(Colors.of(r.readCmColor()));
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayer layer = (DwgLayer) target;
        long self = target.handle();
        layer.setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(self)));
        if (v.from(DwgVersion.R2000)) {
            layer.setPlotStyleHandle(new DwgHandleRef(r.readHandleReference(self)));
        }
        if (v.from(DwgVersion.R2007)) {
            r.readHandleReference(self);            // material
        }
        layer.setLineTypeHandle(new DwgHandleRef(r.readHandleReference(self)));
        if (v.from(DwgVersion.R2013)) {
            // The specification lists an unknown handle that "always seems to be NULL" for all
            // versions. In the samples only R2013+ layers have it, and it is null in all 534.
            r.readHandleReference(self);
        }
    }
}
