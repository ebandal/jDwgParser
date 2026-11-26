package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgViewport;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * VIEWPORT entity (type 34), spec §20.4.38: center 3BD, width BD, height BD, then for R2000+ the
 * fields of the specification's table, kept in the entity's values:
 * <pre>
 *   TARGET, VIEWDIR 3BD, TWIST, VIEWHEIGHT, LENS, FRONTCLIP, BACKCLIP, SNAPANGLE BD,
 *   VIEWCENTER, SNAPBASE, SNAPSPACING, GRIDSPACING 2RD, CIRCLEZOOM BS, R2007+ GRIDMAJOR BS,
 *   FROZENLAYERCOUNT BL, STATUS BL, STYLESHEET TV, RENDERMODE RC, UCSATORIGIN, UCSPERVIEWPORT B,
 *   UCSORIGIN, UCSXDIR, UCSYDIR 3BD, UCSELEVATION BD, UCSORTHOTYPE BS, R2004+ SHADEPLOTMODE BS,
 *   R2007+ DEFAULTLIGHTS B, LIGHTINGTYPE RC, BRIGHTNESS, CONTRAST BD, AMBIENTCOLOR CMC
 *   handles: R13–R14 VPENTHEADER; R2000+ the frozen layers, CLIPBOUNDARY, R2000 VPENTHEADER,
 *            R2000+ NAMEDUCS, BASEUCS, R2007+ BACKGROUND, VISUALSTYLE, SHADEPLOT, SUN
 * </pre>
 */
public class ViewportObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VIEWPORT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgViewport vp = (DwgViewport) target;

        double[] c = r.read3BitDouble();
        vp.setCenter(Point3D.of(c[0], c[1], c[2]));
        vp.setWidth(r.readBitDouble());
        vp.setHeight(r.readBitDouble());
        if (!v.from(DwgVersion.R2000)) {
            return;
        }
        Fields.read(vp, r, "TARGET 3BD", "VIEWDIR 3BD", "TWIST BD", "VIEWHEIGHT BD", "LENS BD",
                "FRONTCLIP BD", "BACKCLIP BD", "SNAPANGLE BD", "VIEWCENTER 2RD", "SNAPBASE 2RD",
                "SNAPSPACING 2RD", "GRIDSPACING 2RD", "CIRCLEZOOM BS");
        vp.setViewHeight((Double) vp.value("VIEWHEIGHT"));
        if (v.from(DwgVersion.R2007)) {
            Fields.read(vp, r, "GRIDMAJOR BS");
        }
        Fields.read(vp, r, "FROZENLAYERCOUNT BL", "STATUS BL", "STYLESHEET TV", "RENDERMODE RC",
                "UCSATORIGIN B", "UCSPERVIEWPORT B", "UCSORIGIN 3BD", "UCSXDIR 3BD", "UCSYDIR 3BD",
                "UCSELEVATION BD", "UCSORTHOTYPE BS");
        if (v.from(DwgVersion.R2004)) {
            Fields.read(vp, r, "SHADEPLOTMODE BS");
        }
        if (v.from(DwgVersion.R2007)) {
            Fields.read(vp, r, "DEFAULTLIGHTS B", "LIGHTINGTYPE RC", "BRIGHTNESS BD", "CONTRAST BD",
                    "AMBIENTCOLOR CMC");
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgViewport vp = (DwgViewport) target;
        long self = target.handle();
        if (!v.from(DwgVersion.R2000)) {
            vp.setValue("VPENTHEADER", handle(r, self));
            return;
        }
        int frozen = vp.value("FROZENLAYERCOUNT") instanceof Integer ? (Integer) vp.value("FROZENLAYERCOUNT") : 0;
        for (int i = 0; i < frozen; i++) {
            vp.addFrozenLayerHandle(handle(r, self));
        }
        vp.setValue("CLIPBOUNDARY", handle(r, self));
        if (!v.from(DwgVersion.R2004)) {
            vp.setValue("VPENTHEADER", handle(r, self));
        }
        vp.setValue("NAMEDUCS", handle(r, self));
        vp.setValue("BASEUCS", handle(r, self));
        if (v.from(DwgVersion.R2007)) {
            vp.setValue("BACKGROUND", handle(r, self));
            vp.setValue("VISUALSTYLE", handle(r, self));
            vp.setValue("SHADEPLOT", handle(r, self));
            vp.setValue("SUN", handle(r, self));
        }
    }

    private static DwgHandleRef handle(BitStreamReader r, long self) {
        return r.readRef(self);
    }
}
