package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDimStyle;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * DIMSTYLE (type 69), spec §20.4.68: the name and table record flags, then the dimension
 * variables in the order and with the types of the specification's table (one list for R13–R14,
 * another for R2000+, with fields added in R2007 and R2010), then an unknown B.
 * One difference from the table, found with the samples: from R2004 the colors DIMCLRD, DIMCLRE
 * and DIMCLRT are CMC, not BS. Read as BS, none of the 437 R2004+ DIMSTYLEs of the samples ends
 * where its data ends; read as CMC, all do (except in one file whose objects are misread
 * throughout) and their values agree with the R2000 copies of the same drawings.
 * Handles: (control, reactors, xdictionary,) xref block, DIMTXSTY; R2000+ DIMLDRBLK, DIMBLK,
 * DIMBLK1, DIMBLK2; R2007+ DIMLTYPE, DIMLTEX1, DIMLTEX2.
 */
public class DimStyleObjectReader implements ObjectReader {

    /** Field types of the specification's table. */
    /** CI: a color index stored as BS, returned as a color like the CMC of later files. */
    private enum Type { B, RC, BS, BD, T, CMC, CI }

    /** A variable, read from files of {@code from} on and before {@code until} (null: no end). */
    private record Field(String name, Type type, DwgVersion from, DwgVersion until) {
        boolean in(DwgVersion v) {
            return v.from(from) && (until == null || !v.from(until));
        }
    }

    private static final Field[] R13_R14 = fields(DwgVersion.R13,
            "DIMTOL B", "DIMLIM B", "DIMTIH B", "DIMTOH B", "DIMSE1 B", "DIMSE2 B", "DIMALT B",
            "DIMTOFL B", "DIMSAH B", "DIMTIX B", "DIMSOXD B", "DIMALTD RC", "DIMZIN RC", "DIMSD1 B",
            "DIMSD2 B", "DIMTOLJ RC", "DIMJUST RC", "DIMFIT RC", "DIMUPT B", "DIMTZIN RC",
            "DIMALTZ RC", "DIMALTTZ RC", "DIMTAD RC", "DIMUNIT BS", "DIMAUNIT BS", "DIMDEC BS",
            "DIMTDEC BS", "DIMALTU BS", "DIMALTTD BS", "DIMSCALE BD", "DIMASZ BD", "DIMEXO BD",
            "DIMDLI BD", "DIMEXE BD", "DIMRND BD", "DIMDLE BD", "DIMTP BD", "DIMTM BD", "DIMTXT BD",
            "DIMCEN BD", "DIMTSZ BD", "DIMALTF BD", "DIMLFAC BD", "DIMTVP BD", "DIMTFAC BD",
            "DIMGAP BD", "DIMPOST T", "DIMAPOST T", "DIMBLK T", "DIMBLK1 T", "DIMBLK2 T",
            "DIMCLRD CI", "DIMCLRE CI", "DIMCLRT CI");

    private static final Field[] R2000_ON = concat(
            fields(DwgVersion.R2000,
                    "DIMPOST T", "DIMAPOST T", "DIMSCALE BD", "DIMASZ BD", "DIMEXO BD", "DIMDLI BD",
                    "DIMEXE BD", "DIMRND BD", "DIMDLE BD", "DIMTP BD", "DIMTM BD"),
            fields(DwgVersion.R2007, "DIMFXL BD", "DIMJOGANG BD", "DIMTFILL BS", "DIMTFILLCLR CMC"),
            fields(DwgVersion.R2000,
                    "DIMTOL B", "DIMLIM B", "DIMTIH B", "DIMTOH B", "DIMSE1 B", "DIMSE2 B",
                    "DIMTAD BS", "DIMZIN BS", "DIMAZIN BS"),
            fields(DwgVersion.R2007, "DIMARCSYM BS"),
            fields(DwgVersion.R2000,
                    "DIMTXT BD", "DIMCEN BD", "DIMTSZ BD", "DIMALTF BD", "DIMLFAC BD", "DIMTVP BD",
                    "DIMTFAC BD", "DIMGAP BD", "DIMALTRND BD", "DIMALT B", "DIMALTD BS", "DIMTOFL B",
                    "DIMSAH B", "DIMTIX B", "DIMSOXD B"),
            range(DwgVersion.R2000, DwgVersion.R2004, "DIMCLRD CI", "DIMCLRE CI", "DIMCLRT CI"),
            fields(DwgVersion.R2004, "DIMCLRD CMC", "DIMCLRE CMC", "DIMCLRT CMC"),
            fields(DwgVersion.R2000,
                    "DIMADEC BS", "DIMDEC BS", "DIMTDEC BS", "DIMALTU BS", "DIMALTTD BS",
                    "DIMAUNIT BS", "DIMFRAC BS", "DIMLUNIT BS", "DIMDSEP BS", "DIMTMOVE BS",
                    "DIMJUST BS", "DIMSD1 B", "DIMSD2 B", "DIMTOLJ BS", "DIMTZIN BS", "DIMALTZ BS",
                    "DIMALTTZ BS", "DIMUPT B", "DIMFIT BS"),
            fields(DwgVersion.R2007, "DIMFXLON B"),
            fields(DwgVersion.R2010, "DIMTXTDIRECTION B", "DIMALTMZF BD", "DIMALTMZS T", "DIMMZF BD",
                    "DIMMZS T"),
            fields(DwgVersion.R2000, "DIMLWD BS", "DIMLWE BS"));

    @Override
    public int objectType() { return DwgObjectType.DIMSTYLE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDimStyle style = (DwgDimStyle) target;
        EntityHeaderReader.readTableRecordName(r, v, style);
        for (Field field : v.from(DwgVersion.R2000) ? R2000_ON : R13_R14) {
            if (field.in(v)) {
                style.set(field.name(), read(field.type(), r));
            }
        }
        r.getInput().readBit();                    // unknown, sets bit 1 of DXF group 70
    }

    private static Object read(Type type, BitStreamReader r) {
        switch (type) {
            case B:   return r.getInput().readBit();
            case RC:  return r.readRawChar();
            case BS:  return r.readBitShort();
            case BD:  return r.readBitDouble();
            case T:   return r.readVariableText();
            case CMC: return Colors.of(r.readCmColor());
            case CI:  return new CmColor(r.readBitShort());
            default:  throw new IllegalStateException(type.name());
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDimStyle style = (DwgDimStyle) target;
        long self = target.handle();
        style.setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(self)));
        style.set("DIMTXSTY", new DwgHandleRef(r.readHandleReference(self)));
        if (v.from(DwgVersion.R2000)) {
            for (String name : new String[] {"DIMLDRBLK", "DIMBLK", "DIMBLK1", "DIMBLK2"}) {
                style.set(name, new DwgHandleRef(r.readHandleReference(self)));
            }
        }
        if (v.from(DwgVersion.R2007)) {
            for (String name : new String[] {"DIMLTYPE", "DIMLTEX1", "DIMLTEX2"}) {
                style.set(name, new DwgHandleRef(r.readHandleReference(self)));
            }
        }
    }

    private static Field[] fields(DwgVersion from, String... specs) {
        return range(from, null, specs);
    }

    private static Field[] range(DwgVersion from, DwgVersion until, String... specs) {
        Field[] fields = new Field[specs.length];
        for (int i = 0; i < specs.length; i++) {
            String[] parts = specs[i].split(" ");
            fields[i] = new Field(parts[0], Type.valueOf(parts[1]), from, until);
        }
        return fields;
    }

    private static Field[] concat(Field[]... groups) {
        return java.util.Arrays.stream(groups).flatMap(java.util.Arrays::stream).toArray(Field[]::new);
    }
}
