package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * VIEW (type 61), spec §20.4.60: the fields of the specification's table, in its order, kept in
 * the record's values under these names: HEIGHT, WIDTH, CENTER (2RD), TARGET, VIEWDIR,
 * TWIST, LENS, FRONTCLIP, BACKCLIP, VIEWMODE (4 bits), R2000+ RENDERMODE, R2007+ DEFAULTLIGHTS,
 * LIGHTINGTYPE, BRIGHTNESS, CONTRAST, AMBIENTCOLOR, then PSPACE, R2000+ ASSOCIATEDUCS and, when
 * it is set, UCSORIGIN, UCSXDIR, UCSYDIR, UCSELEVATION, ORTHOTYPE; R2007+ CAMERAPLOTTABLE.
 * Handles: xref block, R2007+ BACKGROUND, VISUALSTYLE, SUN, R2000+ BASEUCS, NAMEDUCS,
 * R2007+ LIVESECTION. The specification lists the two UCS handles for every R2000+ VIEW; in the
 * samples they are there only when the view has an associated UCS (three views have one and end
 * exactly; the one view without one runs past its end when they are read).
 */
public class ViewObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VIEW.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgTableRecord record = (AbstractDwgTableRecord) target;
        EntityHeaderReader.readTableRecordName(r, v, record);
        Fields.read(record, r, "HEIGHT BD", "WIDTH BD", "CENTER 2RD", "TARGET 3BD", "VIEWDIR 3BD",
                "TWIST BD", "LENS BD", "FRONTCLIP BD", "BACKCLIP BD", "VIEWMODE X4");
        if (v.from(DwgVersion.R2000)) {
            Fields.read(record, r, "RENDERMODE RC");
        }
        if (v.from(DwgVersion.R2007)) {
            Fields.read(record, r, "DEFAULTLIGHTS B", "LIGHTINGTYPE RC", "BRIGHTNESS BD", "CONTRAST BD",
                    "AMBIENTCOLOR CMC");
        }
        Fields.read(record, r, "PSPACE B");
        if (v.from(DwgVersion.R2000)) {
            Fields.read(record, r, "ASSOCIATEDUCS B");
            if (Boolean.TRUE.equals(record.value("ASSOCIATEDUCS"))) {
                Fields.read(record, r, "UCSORIGIN 3BD", "UCSXDIR 3BD", "UCSYDIR 3BD", "UCSELEVATION BD",
                        "ORTHOTYPE BS");
            }
        }
        if (v.from(DwgVersion.R2007)) {
            Fields.read(record, r, "CAMERAPLOTTABLE B");
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgTableRecord record = (AbstractDwgTableRecord) target;
        long self = target.handle();
        record.setXrefBlockHandle(r.readRef(self));
        if (v.from(DwgVersion.R2007)) {
            handle(record, "BACKGROUND", r, self);
            handle(record, "VISUALSTYLE", r, self);
            handle(record, "SUN", r, self);
        }
        if (v.from(DwgVersion.R2000) && Boolean.TRUE.equals(record.value("ASSOCIATEDUCS"))) {
            handle(record, "BASEUCS", r, self);
            handle(record, "NAMEDUCS", r, self);
        }
        if (v.from(DwgVersion.R2007)) {
            handle(record, "LIVESECTION", r, self);
        }
    }

    private static void handle(AbstractDwgTableRecord record, String name, BitStreamReader r, long self) {
        record.setValue(name, r.readRef(self));
    }
}
