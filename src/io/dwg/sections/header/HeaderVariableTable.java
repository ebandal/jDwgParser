package io.dwg.sections.header;

import io.dwg.core.version.DwgVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The header variables of spec chapter 9, in the order of the specification's list. Each line:
 * the versions the variable is present in, its type, and its name (the DXF name without the $;
 * a P prefix for the paper space ones, UNKNOWN_n for the unnamed ones).
 * <p>Versions: {@code *} all; {@code A-B} from A to B; {@code A+} from A on; {@code A} only A.
 * Types are those of chapter 2; {@code HD} is a handle stored in the data stream rather than in
 * the handle stream (HANDSEED); {@code BSE} and {@code RCE} are a BS and an RC stored at the end
 * of the last stream (R2007+: after the handles).
 * <p>Where this differs from the specification's list, found on the samples. With these, the
 * headers of 251 of the 253 samples end their streams exactly (one R13 file has 12 bits and one
 * R2000 file a byte more), all 10,167 header handles are null or objects of the file, and the
 * values agree with the copies of the same drawing in other versions:
 * <ul>
 *   <li>MENUNAME is there in every version (the list: R13 to R2004). In R2007+ without it all
 *       strings after it are one off.</li>
 *   <li>The 4 unknown shorts at the end ("R14+") are there in R13 too, and in R2007+ they come
 *       after the handles: read in the data stream they run 56 bits past it.</li>
 *   <li>R2007 and R2010 end with one more byte, 0xAF in all 117 files.</li>
 * </ul>
 */
final class HeaderVariableTable {

    /** One variable: present from {@code from} to {@code to} (inclusive). */
    static final class Entry {
        private final DwgVersion from;
        private final DwgVersion to;
        private final String type;
        private final String name;

        Entry(DwgVersion from, DwgVersion to, String type, String name) {
            this.from = from;
            this.to = to;
            this.type = type;
            this.name = name;
        }

        public DwgVersion from() {
            return from;
        }

        public DwgVersion to() {
            return to;
        }

        public String type() {
            return type;
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry other = (Entry) o;
            return java.util.Objects.equals(from, other.from)
                    && java.util.Objects.equals(to, other.to)
                    && java.util.Objects.equals(type, other.type)
                    && java.util.Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(from, to, type, name);
        }

        @Override
        public String toString() {
            return "Entry[" + "from=" + from + ", " + "to=" + to + ", " + "type=" + type + ", " + "name=" + name + "]";
        }

        boolean presentIn(DwgVersion v) {
            DwgVersion after = to == null ? null : next(to);
            return v.from(from) && (after == null || !v.from(after));
        }
    }

    private static final String[] TABLE = {
            "R2007     RL   SIZEINBITS",
            "R2013+    BLL  REQUIREDVERSIONS",
            "*         BD   UNKNOWN_1",
            "*         BD   UNKNOWN_2",
            "*         BD   UNKNOWN_3",
            "*         BD   UNKNOWN_4",
            "*         TV   UNKNOWN_5",
            "*         TV   UNKNOWN_6",
            "*         TV   UNKNOWN_7",
            "*         TV   UNKNOWN_8",
            "*         BL   UNKNOWN_9",
            "*         BL   UNKNOWN_10",
            "R13-R14   BS   UNKNOWN_11",
            "R13-R2000 H    CURRENTVIEWPORTENTITYHEADER",
            "*         B    DIMASO",
            "*         B    DIMSHO",
            "R13-R14   B    DIMSAV",
            "*         B    PLINEGEN",
            "*         B    ORTHOMODE",
            "*         B    REGENMODE",
            "*         B    FILLMODE",
            "*         B    QTEXTMODE",
            "*         B    PSLTSCALE",
            "*         B    LIMCHECK",
            "R13-R14   B    BLIPMODE",
            "R2004+    B    UNKNOWN_12",
            "*         B    USRTIMER",
            "*         B    SKPOLY",
            "*         B    ANGDIR",
            "*         B    SPLFRAME",
            "R13-R14   B    ATTREQ",
            "R13-R14   B    ATTDIA",
            "*         B    MIRRTEXT",
            "*         B    WORLDVIEW",
            "R13-R14   B    WIREFRAME",
            "*         B    TILEMODE",
            "*         B    PLIMCHECK",
            "*         B    VISRETAIN",
            "R13-R14   B    DELOBJ",
            "*         B    DISPSILH",
            "*         B    PELLIPSE",
            "*         BS   PROXYGRAPHICS",
            "R13-R14   BS   DRAGMODE",
            "*         BS   TREEDEPTH",
            "*         BS   LUNITS",
            "*         BS   LUPREC",
            "*         BS   AUNITS",
            "*         BS   AUPREC",
            "R13-R14   BS   OSMODE",
            "*         BS   ATTMODE",
            "R13-R14   BS   COORDS",
            "*         BS   PDMODE",
            "R13-R14   BS   PICKSTYLE",
            "R2004+    BL   UNKNOWN_13",
            "R2004+    BL   UNKNOWN_14",
            "R2004+    BL   UNKNOWN_15",
            "*         BS   USERI1",
            "*         BS   USERI2",
            "*         BS   USERI3",
            "*         BS   USERI4",
            "*         BS   USERI5",
            "*         BS   SPLINESEGS",
            "*         BS   SURFU",
            "*         BS   SURFV",
            "*         BS   SURFTYPE",
            "*         BS   SURFTAB1",
            "*         BS   SURFTAB2",
            "*         BS   SPLINETYPE",
            "*         BS   SHADEDGE",
            "*         BS   SHADEDIF",
            "*         BS   UNITMODE",
            "*         BS   MAXACTVP",
            "*         BS   ISOLINES",
            "*         BS   CMLJUST",
            "*         BS   TEXTQLTY",
            "*         BD   LTSCALE",
            "*         BD   TEXTSIZE",
            "*         BD   TRACEWID",
            "*         BD   SKETCHINC",
            "*         BD   FILLETRAD",
            "*         BD   THICKNESS",
            "*         BD   ANGBASE",
            "*         BD   PDSIZE",
            "*         BD   PLINEWID",
            "*         BD   USERR1",
            "*         BD   USERR2",
            "*         BD   USERR3",
            "*         BD   USERR4",
            "*         BD   USERR5",
            "*         BD   CHAMFERA",
            "*         BD   CHAMFERB",
            "*         BD   CHAMFERC",
            "*         BD   CHAMFERD",
            "*         BD   FACETRES",
            "*         BD   CMLSCALE",
            "*         BD   CELTSCALE",
            "*         TV   MENUNAME",
            "*         BL   TDCREATE_DAY",
            "*         BL   TDCREATE_MS",
            "*         BL   TDUPDATE_DAY",
            "*         BL   TDUPDATE_MS",
            "R2004+    BL   UNKNOWN_16",
            "R2004+    BL   UNKNOWN_17",
            "R2004+    BL   UNKNOWN_18",
            "*         BL   TDINDWG_DAY",
            "*         BL   TDINDWG_MS",
            "*         BL   TDUSRTIMER_DAY",
            "*         BL   TDUSRTIMER_MS",
            "*         CMC  CECOLOR",
            "*         HD   HANDSEED",
            "*         H    CLAYER",
            "*         H    TEXTSTYLE",
            "*         H    CELTYPE",
            "R2007+    H    CMATERIAL",
            "*         H    DIMSTYLE",
            "*         H    CMLSTYLE",
            "R2000+    BD   PSVPSCALE",
            "*         3BD  PINSBASE",
            "*         3BD  PEXTMIN",
            "*         3BD  PEXTMAX",
            "*         2RD  PLIMMIN",
            "*         2RD  PLIMMAX",
            "*         BD   PELEVATION",
            "*         3BD  PUCSORG",
            "*         3BD  PUCSXDIR",
            "*         3BD  PUCSYDIR",
            "*         H    PUCSNAME",
            "R2000+    H    PUCSORTHOREF",
            "R2000+    BS   PUCSORTHOVIEW",
            "R2000+    H    PUCSBASE",
            "R2000+    3BD  PUCSORGTOP",
            "R2000+    3BD  PUCSORGBOTTOM",
            "R2000+    3BD  PUCSORGLEFT",
            "R2000+    3BD  PUCSORGRIGHT",
            "R2000+    3BD  PUCSORGFRONT",
            "R2000+    3BD  PUCSORGBACK",
            "*         3BD  INSBASE",
            "*         3BD  EXTMIN",
            "*         3BD  EXTMAX",
            "*         2RD  LIMMIN",
            "*         2RD  LIMMAX",
            "*         BD   ELEVATION",
            "*         3BD  UCSORG",
            "*         3BD  UCSXDIR",
            "*         3BD  UCSYDIR",
            "*         H    UCSNAME",
            "R2000+    H    UCSORTHOREF",
            "R2000+    BS   UCSORTHOVIEW",
            "R2000+    H    UCSBASE",
            "R2000+    3BD  UCSORGTOP",
            "R2000+    3BD  UCSORGBOTTOM",
            "R2000+    3BD  UCSORGLEFT",
            "R2000+    3BD  UCSORGRIGHT",
            "R2000+    3BD  UCSORGFRONT",
            "R2000+    3BD  UCSORGBACK",
            "R2000+    TV   DIMPOST",
            "R2000+    TV   DIMAPOST",
            "R13-R14   B    DIMTOL",
            "R13-R14   B    DIMLIM",
            "R13-R14   B    DIMTIH",
            "R13-R14   B    DIMTOH",
            "R13-R14   B    DIMSE1",
            "R13-R14   B    DIMSE2",
            "R13-R14   B    DIMALT",
            "R13-R14   B    DIMTOFL",
            "R13-R14   B    DIMSAH",
            "R13-R14   B    DIMTIX",
            "R13-R14   B    DIMSOXD",
            "R13-R14   RC   DIMALTD",
            "R13-R14   RC   DIMZIN",
            "R13-R14   B    DIMSD1",
            "R13-R14   B    DIMSD2",
            "R13-R14   RC   DIMTOLJ",
            "R13-R14   RC   DIMJUST",
            "R13-R14   RC   DIMFIT",
            "R13-R14   B    DIMUPT",
            "R13-R14   RC   DIMTZIN",
            "R13-R14   RC   DIMALTZ",
            "R13-R14   RC   DIMALTTZ",
            "R13-R14   RC   DIMTAD",
            "R13-R14   BS   DIMUNIT",
            "R13-R14   BS   DIMAUNIT",
            "R13-R14   BS   DIMDEC",
            "R13-R14   BS   DIMTDEC",
            "R13-R14   BS   DIMALTU",
            "R13-R14   BS   DIMALTTD",
            "R13-R14   H    DIMTXSTY",
            "*         BD   DIMSCALE",
            "*         BD   DIMASZ",
            "*         BD   DIMEXO",
            "*         BD   DIMDLI",
            "*         BD   DIMEXE",
            "*         BD   DIMRND",
            "*         BD   DIMDLE",
            "*         BD   DIMTP",
            "*         BD   DIMTM",
            "R2007+    BD   DIMFXL",
            "R2007+    BD   DIMJOGANG",
            "R2007+    BS   DIMTFILL",
            "R2007+    CMC  DIMTFILLCLR",
            "R2000+    B    DIMTOL",
            "R2000+    B    DIMLIM",
            "R2000+    B    DIMTIH",
            "R2000+    B    DIMTOH",
            "R2000+    B    DIMSE1",
            "R2000+    B    DIMSE2",
            "R2000+    BS   DIMTAD",
            "R2000+    BS   DIMZIN",
            "R2000+    BS   DIMAZIN",
            "R2007+    BS   DIMARCSYM",
            "*         BD   DIMTXT",
            "*         BD   DIMCEN",
            "*         BD   DIMTSZ",
            "*         BD   DIMALTF",
            "*         BD   DIMLFAC",
            "*         BD   DIMTVP",
            "*         BD   DIMTFAC",
            "*         BD   DIMGAP",
            "R13-R14   TV   DIMPOST",
            "R13-R14   TV   DIMAPOST",
            "R13-R14   TV   DIMBLK",
            "R13-R14   TV   DIMBLK1",
            "R13-R14   TV   DIMBLK2",
            "R2000+    BD   DIMALTRND",
            "R2000+    B    DIMALT",
            "R2000+    BS   DIMALTD",
            "R2000+    B    DIMTOFL",
            "R2000+    B    DIMSAH",
            "R2000+    B    DIMTIX",
            "R2000+    B    DIMSOXD",
            "*         CMC  DIMCLRD",
            "*         CMC  DIMCLRE",
            "*         CMC  DIMCLRT",
            "R2000+    BS   DIMADEC",
            "R2000+    BS   DIMDEC",
            "R2000+    BS   DIMTDEC",
            "R2000+    BS   DIMALTU",
            "R2000+    BS   DIMALTTD",
            "R2000+    BS   DIMAUNIT",
            "R2000+    BS   DIMFRAC",
            "R2000+    BS   DIMLUNIT",
            "R2000+    BS   DIMDSEP",
            "R2000+    BS   DIMTMOVE",
            "R2000+    BS   DIMJUST",
            "R2000+    B    DIMSD1",
            "R2000+    B    DIMSD2",
            "R2000+    BS   DIMTOLJ",
            "R2000+    BS   DIMTZIN",
            "R2000+    BS   DIMALTZ",
            "R2000+    BS   DIMALTTZ",
            "R2000+    B    DIMUPT",
            "R2000+    BS   DIMATFIT",
            "R2007+    B    DIMFXLON",
            "R2010+    B    DIMTXTDIRECTION",
            "R2010+    BD   DIMALTMZF",
            "R2010+    TV   DIMALTMZS",
            "R2010+    BD   DIMMZF",
            "R2010+    TV   DIMMZS",
            "R2000+    H    DIMTXSTY",
            "R2000+    H    DIMLDRBLK",
            "R2000+    H    DIMBLK",
            "R2000+    H    DIMBLK1",
            "R2000+    H    DIMBLK2",
            "R2007+    H    DIMLTYPE",
            "R2007+    H    DIMLTEX1",
            "R2007+    H    DIMLTEX2",
            "R2000+    BS   DIMLWD",
            "R2000+    BS   DIMLWE",
            "*         H    BLOCK_CONTROL_OBJECT",
            "*         H    LAYER_CONTROL_OBJECT",
            "*         H    STYLE_CONTROL_OBJECT",
            "*         H    LINETYPE_CONTROL_OBJECT",
            "*         H    VIEW_CONTROL_OBJECT",
            "*         H    UCS_CONTROL_OBJECT",
            "*         H    VPORT_CONTROL_OBJECT",
            "*         H    APPID_CONTROL_OBJECT",
            "*         H    DIMSTYLE_CONTROL_OBJECT",
            "R13-R2000 H    VIEWPORT_ENTITY_HEADER_CONTROL_OBJECT",
            "*         H    DICTIONARY_ACAD_GROUP",
            "*         H    DICTIONARY_ACAD_MLINESTYLE",
            "*         H    DICTIONARY_NAMED_OBJECTS",
            "R2000+    BS   TSTACKALIGN",
            "R2000+    BS   TSTACKSIZE",
            "R2000+    TV   HYPERLINKBASE",
            "R2000+    TV   STYLESHEET",
            "R2000+    H    DICTIONARY_LAYOUTS",
            "R2000+    H    DICTIONARY_PLOTSETTINGS",
            "R2000+    H    DICTIONARY_PLOTSTYLES",
            "R2004+    H    DICTIONARY_MATERIALS",
            "R2004+    H    DICTIONARY_COLORS",
            "R2007+    H    DICTIONARY_VISUALSTYLE",
            "R2013+    H    UNKNOWN_19",
            "R2000+    BL   FLAGS",
            "R2000+    BS   INSUNITS",
            "R2000+    BS   CEPSNTYPE",
            "R2000+    H    CPSNID",
            "R2000+    TV   FINGERPRINTGUID",
            "R2000+    TV   VERSIONGUID",
            "R2004+    RC   SORTENTS",
            "R2004+    RC   INDEXCTL",
            "R2004+    RC   HIDETEXT",
            "R2004+    RC   XCLIPFRAME",
            "R2004+    RC   DIMASSOC",
            "R2004+    RC   HALOGAP",
            "R2004+    BS   OBSCUREDCOLOR",
            "R2004+    BS   INTERSECTIONCOLOR",
            "R2004+    RC   OBSCUREDLTYPE",
            "R2004+    RC   INTERSECTIONDISPLAY",
            "R2004+    TV   PROJECTNAME",
            "*         H    BLOCK_RECORD_PAPER_SPACE",
            "*         H    BLOCK_RECORD_MODEL_SPACE",
            "*         H    LTYPE_BYLAYER",
            "*         H    LTYPE_BYBLOCK",
            "*         H    LTYPE_CONTINUOUS",
            "R2007+    B    CAMERADISPLAY",
            "R2007+    BL   UNKNOWN_20",
            "R2007+    BL   UNKNOWN_21",
            "R2007+    BD   UNKNOWN_22",
            "R2007+    BD   STEPSPERSEC",
            "R2007+    BD   STEPSIZE",
            "R2007+    BD   3DDWFPREC",
            "R2007+    BD   LENSLENGTH",
            "R2007+    BD   CAMERAHEIGHT",
            "R2007+    RC   SOLIDHIST",
            "R2007+    RC   SHOWHIST",
            "R2007+    BD   PSOLWIDTH",
            "R2007+    BD   PSOLHEIGHT",
            "R2007+    BD   LOFTANG1",
            "R2007+    BD   LOFTANG2",
            "R2007+    BD   LOFTMAG1",
            "R2007+    BD   LOFTMAG2",
            "R2007+    BS   LOFTPARAM",
            "R2007+    RC   LOFTNORMALS",
            "R2007+    BD   LATITUDE",
            "R2007+    BD   LONGITUDE",
            "R2007+    BD   NORTHDIRECTION",
            "R2007+    BL   TIMEZONE",
            "R2007+    RC   LIGHTGLYPHDISPLAY",
            "R2007+    RC   TILEMODELIGHTSYNCH",
            "R2007+    RC   DWFFRAME",
            "R2007+    RC   DGNFRAME",
            "R2007+    B    UNKNOWN_23",
            "R2007+    CMC  INTERFERECOLOR",
            "R2007+    H    INTERFEREOBJVS",
            "R2007+    H    INTERFEREVPVS",
            "R2007+    H    DRAGVS",
            "R2007+    RC   CSHADOW",
            "R2007+    BD   UNKNOWN_24",
            "*         BSE  UNKNOWN_25",
            "*         BSE  UNKNOWN_26",
            "*         BSE  UNKNOWN_27",
            "*         BSE  UNKNOWN_28",
            "R2007-R2010 RCE UNKNOWN_29",
    };

    static final List<Entry> ENTRIES = parse();

    private HeaderVariableTable() {
    }

    private static List<Entry> parse() {
        List<Entry> list = new ArrayList<>();
        for (String line : TABLE) {
            String[] f = line.trim().split("\\s+");
            if (f.length != 3) {
                continue;
            }
            DwgVersion from;
            DwgVersion to;
            String v = f[0];
            if (v.equals("*")) {
                from = DwgVersion.R13;
                to = null;
            } else if (v.endsWith("+")) {
                from = version(v.substring(0, v.length() - 1));
                to = null;
            } else if (v.contains("-")) {
                from = version(v.substring(0, v.indexOf('-')));
                to = version(v.substring(v.indexOf('-') + 1));
            } else {
                from = version(v);
                to = from;
            }
            list.add(new Entry(from, to, f[1], f[2]));
        }
        return Collections.unmodifiableList(list);
    }

    private static DwgVersion version(String name) {
        return DwgVersion.valueOf(name);
    }

    /** The version after {@code v}. */
    private static DwgVersion next(DwgVersion v) {
        DwgVersion[] all = DwgVersion.values();
        for (int i = 0; i < all.length - 1; i++) {
            if (all[i] == v) {
                return all[i + 1];
            }
        }
        return null;
    }
}
