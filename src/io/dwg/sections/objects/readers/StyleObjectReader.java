package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgStyle;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * STYLE (type 53, SHAPEFILE in spec §20.4.56).
 * <pre>
 *   name TV, table record flags, vertical B, shape file B, fixed height BD, width factor BD,
 *   oblique angle BD, generation RC, last height BD, font name TV, big font name TV
 *   handles: (control, reactors, xdictionary,) xref block
 * </pre>
 */
public class StyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.STYLE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgStyle style = (DwgStyle) target;
        EntityHeaderReader.readTableRecordName(r, v, style);
        style.setVertical(r.getInput().readBit());
        style.setShapeFile(r.getInput().readBit());
        style.setFixedHeight(r.readBitDouble());
        style.setWidth(r.readBitDouble());
        style.setOblique(r.readBitDouble());
        style.setGeneration(r.readRawChar());
        style.setLastHeight(r.readBitDouble());
        style.setFontFilename(r.readVariableText());
        style.setBigFontFilename(r.readVariableText());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgStyle) target).setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
