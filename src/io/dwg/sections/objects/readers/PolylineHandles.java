package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgPolyline;

/**
 * The vertex handles of the old-style POLYLINEs (spec §20.4.16, §20.4.17, §20.4.33, §20.4.34):
 * R13–R2000 first and last VERTEX, R2004+ one VERTEX per owned object, then SEQEND.
 */
final class PolylineHandles {

    private PolylineHandles() {
    }

    /** R2004+: the owned object count at the end of the data. */
    static void readOwnedCount(AbstractDwgPolyline polyline, BitStreamReader r, DwgVersion v) {
        if (v.from(DwgVersion.R2004)) {
            polyline.setOwnedObjectCount(r.readBitLong());
        }
    }

    static void read(AbstractDwgPolyline polyline, BitStreamReader r, DwgVersion v) {
        long self = polyline.handle();
        if (v.from(DwgVersion.R2004)) {
            for (int i = 0; i < polyline.ownedObjectCount(); i++) {
                polyline.addVertexHandle(r.readRef(self));
            }
        } else {
            polyline.setFirstVertexHandle(r.readRef(self));
            polyline.setLastVertexHandle(r.readRef(self));
        }
        polyline.setSeqendHandle(r.readRef(self));
    }
}
