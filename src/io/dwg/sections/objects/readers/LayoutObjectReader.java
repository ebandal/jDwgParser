package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLayout;
import io.dwg.sections.objects.ObjectReader;

/**
 * LAYOUT (a class), spec §20.4.84: the plot settings, then the layout fields, in the order and
 * with the types of the specification's table, kept in the object's values.
 * <pre>
 *   PAGESETUP TV, PRINTER TV, PLOTFLAGS BS, LEFTMARGIN, BOTTOMMARGIN, RIGHTMARGIN, TOPMARGIN,
 *   PAPERWIDTH, PAPERHEIGHT BD, PAPERSIZE TV, PLOTORIGIN 2BD, PAPERUNITS, PLOTROTATION, PLOTTYPE BS,
 *   WINDOWMIN, WINDOWMAX 2BD, R13–R2000 PLOTVIEWNAME T, REALWORLDUNITS, DRAWINGUNITS BD,
 *   STYLESHEET TV, SCALETYPE BS, SCALEFACTOR BD, PAPERIMAGEORIGIN 2BD,
 *   R2004+ SHADEPLOTMODE, SHADEPLOTRESLEVEL, SHADEPLOTDPI BS,
 *   LAYOUTNAME TV, TABORDER BL, LAYOUTFLAGS BS, UCSORIGIN 3BD, LIMMIN, LIMMAX 2RD, INSBASE 3BD,
 *   UCSXDIR, UCSYDIR 3BD, ELEVATION BD, ORTHOTYPE BS, EXTMIN, EXTMAX 3BD, R2004+ VIEWPORTCOUNT BL
 *   handles: (parent, reactors, xdictionary,) R2004+ PLOTVIEW, R2007+ VISUALSTYLE, BLOCK,
 *   LASTVIEWPORT, BASEUCS, NAMEDUCS, R2004+ one per viewport
 * </pre>
 * The specification gives the viewport count as RL; it is a BL. Read as RL, every R2004+ LAYOUT
 * of the samples runs about 32 bits past its data; read as BL, all 340 end their data and their
 * handles exactly.
 */
public class LayoutObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LAYOUT.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayout layout = (DwgLayout) target;
        Fields.read(layout, r, "PAGESETUP TV", "PRINTER TV", "PLOTFLAGS BS", "LEFTMARGIN BD",
                "BOTTOMMARGIN BD", "RIGHTMARGIN BD", "TOPMARGIN BD", "PAPERWIDTH BD", "PAPERHEIGHT BD",
                "PAPERSIZE TV", "PLOTORIGIN 2BD", "PAPERUNITS BS", "PLOTROTATION BS", "PLOTTYPE BS",
                "WINDOWMIN 2BD", "WINDOWMAX 2BD");
        if (!v.from(DwgVersion.R2004)) {
            Fields.read(layout, r, "PLOTVIEWNAME TV");
        }
        Fields.read(layout, r, "REALWORLDUNITS BD", "DRAWINGUNITS BD", "STYLESHEET TV", "SCALETYPE BS",
                "SCALEFACTOR BD", "PAPERIMAGEORIGIN 2BD");
        if (v.from(DwgVersion.R2004)) {
            Fields.read(layout, r, "SHADEPLOTMODE BS", "SHADEPLOTRESLEVEL BS", "SHADEPLOTDPI BS");
        }
        Fields.read(layout, r, "LAYOUTNAME TV", "TABORDER BL", "LAYOUTFLAGS BS", "UCSORIGIN 3BD",
                "LIMMIN 2RD", "LIMMAX 2RD", "INSBASE 3BD", "UCSXDIR 3BD", "UCSYDIR 3BD", "ELEVATION BD",
                "ORTHOTYPE BS", "EXTMIN 3BD", "EXTMAX 3BD");
        if (v.from(DwgVersion.R2004)) {
            Fields.read(layout, r, "VIEWPORTCOUNT BL");
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayout layout = (DwgLayout) target;
        long self = target.handle();
        if (v.from(DwgVersion.R2004)) {
            layout.setValue("PLOTVIEW", handle(r, self));
        }
        if (v.from(DwgVersion.R2007)) {
            layout.setValue("VISUALSTYLE", handle(r, self));
        }
        layout.setValue("BLOCK", handle(r, self));
        layout.setValue("LASTVIEWPORT", handle(r, self));
        layout.setValue("BASEUCS", handle(r, self));
        layout.setValue("NAMEDUCS", handle(r, self));
        if (v.from(DwgVersion.R2004) && layout.value("VIEWPORTCOUNT") instanceof Integer) {
            int count = (Integer) layout.value("VIEWPORTCOUNT");
            for (int i = 0; i < count; i++) {
                layout.addViewportHandle(handle(r, self));
            }
        }
    }

    private static DwgHandleRef handle(BitStreamReader r, long self) {
        return r.readRef(self);
    }
}
