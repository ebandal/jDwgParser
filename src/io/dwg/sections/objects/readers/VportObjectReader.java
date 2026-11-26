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
 * VPORT (type 65), spec §20.4.64: the fields of the specification's table, in its order, kept in
 * the record's values: HEIGHT, ASPECT (times the height, as stored), CENTER (2RD), TARGET,
 * VIEWDIR, TWIST, LENS, FRONTCLIP, BACKCLIP, VIEWMODE (4 bits), R2000+ RENDERMODE, R2007+
 * DEFAULTLIGHTS, LIGHTINGTYPE, BRIGHTNESS, CONTRAST, AMBIENTCOLOR, then LOWERLEFT, UPPERRIGHT,
 * UCSFOLLOW, CIRCLEZOOM, FASTZOOM, UCSICON (2 bits), GRID, GRIDSPACING, SNAP, SNAPSTYLE,
 * SNAPISOPAIR, SNAPROTATION, SNAPBASE, SNAPSPACING, R2000+ an unknown B, UCSPERVIEWPORT,
 * UCSORIGIN, UCSXDIR, UCSYDIR, UCSELEVATION, UCSORTHOTYPE, R2007+ GRIDFLAGS, GRIDMAJOR.
 * Handles: xref block, R2007+ BACKGROUND, VISUALSTYLE, SUN, R2000+ NAMEDUCS, BASEUCS.
 */
public class VportObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VPORT.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgTableRecord record = (AbstractDwgTableRecord) target;
        EntityHeaderReader.readTableRecordName(r, v, record);
        Fields.read(record, r, "HEIGHT BD", "ASPECT BD", "CENTER 2RD", "TARGET 3BD", "VIEWDIR 3BD",
                "TWIST BD", "LENS BD", "FRONTCLIP BD", "BACKCLIP BD", "VIEWMODE X4");
        if (v.from(DwgVersion.R2000)) {
            Fields.read(record, r, "RENDERMODE RC");
        }
        if (v.from(DwgVersion.R2007)) {
            Fields.read(record, r, "DEFAULTLIGHTS B", "LIGHTINGTYPE RC", "BRIGHTNESS BD", "CONTRAST BD",
                    "AMBIENTCOLOR CMC");
        }
        Fields.read(record, r, "LOWERLEFT 2RD", "UPPERRIGHT 2RD", "UCSFOLLOW B", "CIRCLEZOOM BS",
                "FASTZOOM B", "UCSICON X2", "GRID B", "GRIDSPACING 2RD", "SNAP B", "SNAPSTYLE B",
                "SNAPISOPAIR BS", "SNAPROTATION BD", "SNAPBASE 2RD", "SNAPSPACING 2RD");
        if (v.from(DwgVersion.R2000)) {
            r.getInput().readBit();                // unknown
            Fields.read(record, r, "UCSPERVIEWPORT B", "UCSORIGIN 3BD", "UCSXDIR 3BD", "UCSYDIR 3BD",
                    "UCSELEVATION BD", "UCSORTHOTYPE BS");
        }
        if (v.from(DwgVersion.R2007)) {
            Fields.read(record, r, "GRIDFLAGS BS", "GRIDMAJOR BS");
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
        if (v.from(DwgVersion.R2000)) {
            handle(record, "NAMEDUCS", r, self);
            handle(record, "BASEUCS", r, self);
        }
    }

    private static void handle(AbstractDwgTableRecord record, String name, BitStreamReader r, long self) {
        record.setValue(name, r.readRef(self));
    }
}
