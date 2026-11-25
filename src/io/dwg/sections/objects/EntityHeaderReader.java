package io.dwg.sections.objects;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgTableRecord;

/**
 * Reads the common start of objects and entities, after the object type.
 *
 * <p>Spec §20.1 (objects) and §20.2 / §20.4.1 (entities):
 * <pre>
 *   R2000–R2007  RL  size of the object data in bits (R2010+ give the handle stream size
 *                    before the type instead)
 *                H   the object's handle
 *                EED (chapter 28): repeated BS length, application handle, length bytes,
 *                    until a length of 0
 *   objects:     R13–R14 RL size in bits, BL reactor count, R2004+ B XDic missing,
 *                R2013+ B has DS binary data
 *   entities:    the common entity data, see {@link #readCommonEntityData}
 * </pre>
 * The data is a bit stream: raw bytes are read at the current bit position. The values the
 * handle stream needs later are kept in {@link ObjectHeaderData}.
 */
public class EntityHeaderReader {

    /** The bit of the R2007+ table record flags that marks an xref-dependent record. */
    private static final int XREF_DEPENDENT_R2007 = 256;

    /** Reads the object data size (R2000–R2007), the handle and the EED. */
    public static void readEntityHeader(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);
        if (v.from(DwgVersion.R2000) && !v.from(DwgVersion.R2010)) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }
        readHandle(input);
        readExtendedData(r, header);
    }

    /**
     * EED (chapter 28): BS length, application handle, then length bytes; ends at length 0.
     * The bytes are kept in {@link ObjectHeaderData#extendedData} and decoded later.
     */
    private static void readExtendedData(BitStreamReader r, ObjectHeaderData header) {
        BitInput input = r.getInput();
        for (int length = r.readBitShort(); length > 0 && length <= 0x7FFF; length = r.readBitShort()) {
            long appHandle = r.readHandleReference(0);
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) input.readBits(8);
            }
            header.extendedData.add(new ObjectHeaderData.ExtendedData(appHandle, data));
        }
    }

    /** Reads the common start of a non-entity object (§20.1), up to its own data. */
    public static void readObjectHeader(BitStreamReader r, DwgVersion v) throws Exception {
        readEntityHeader(r, v);
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000)) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }
        header.numReactors = r.readBitLong();
        if (v.from(DwgVersion.R2004)) {
            header.xdicMissing = input.readBit();
        }
        if (v.from(DwgVersion.R2013)) {
            input.readBit();                       // has DS binary data
        }
    }

    /**
     * Reads the flags after the name of a table record (LAYER, BLOCK_HEADER, ...). Spec
     * (e.g. §20.4.54): 64-flag (B), xref index + 1 (BS), xdep (B). R2007+ files hold only one
     * BS, which the specification does not describe; read as it describes, the R2007+ samples no
     * longer agree with their R2000 counterparts. Measured on the samples: the BS is 256 for
     * each of the 78 xref-dependent records (names with '|', an xref block handle) and 0 for all
     * others, also for those whose R2000 copy has the 64-flag set. So R2007+ files give only the
     * xref-dependent flag; the 64-flag is false and the xref index -1 there.
     */
    public static void readTableRecordFlags(BitStreamReader r, DwgVersion v, AbstractDwgTableRecord record) {
        if (v.from(DwgVersion.R2007)) {
            int flags = r.readBitShort();
            record.setXrefDependent((flags & XREF_DEPENDENT_R2007) != 0);
            return;
        }
        record.setReferenced(r.getInput().readBit());
        record.setXrefIndex(r.readBitShort() - 1);
        record.setXrefDependent(r.getInput().readBit());
    }

    /** Reads the name (TV) of a table record and the flags that follow it. */
    public static void readTableRecordName(BitStreamReader r, DwgVersion v, AbstractDwgTableRecord record) {
        record.setName(r.readVariableText());
        readTableRecordFlags(r, v, record);
    }

    /**
     * Reads the common entity data of §20.4.1 that follows the EED, into
     * {@link ObjectHeaderData}. Two points the table leaves open are settled by the sample
     * files (R2004+ files render like their R2000 counterparts only this way): Nolinks is present
     * only up to R2000, and R2010+ have three visual style bits before the invisibility (the
     * handle data of §20.4.2 refers to them).
     */
    public static void readCommonEntityData(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);
        header.entity = true;
        boolean r13r14 = v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000);

        if (input.readBit()) {                     // graphic present
            long size = v.from(DwgVersion.R2010) ? r.readBitLongLong() : readRawLong(input);
            r.seek(r.position() + size * 8L);
        }
        if (r13r14) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }
        header.entMode = input.readBits(2);
        header.numReactors = r.readBitLong();
        if (r13r14) {
            header.isByLayerLinetype = input.readBit();
        }
        if (v.from(DwgVersion.R2004)) {
            header.xdicMissing = input.readBit();
        }
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2004)) {
            header.noLinks = input.readBit();
        }
        if (v.from(DwgVersion.R2013)) {
            input.readBit();                       // has DS binary data
        }
        if (v.from(DwgVersion.R2004)) {
            readEncColor(r, v, header);
        } else if (v.from(DwgVersion.R13)) {
            header.colorIndex = r.readBitShort();
        }
        if (v.from(DwgVersion.R13)) {
            header.linetypeScale = r.readBitDouble();
        }
        if (v.from(DwgVersion.R2000)) {
            header.linetypeFlags = input.readBits(2);
            header.plotStyleFlags = input.readBits(2);
        }
        if (v.from(DwgVersion.R2007)) {
            header.materialFlags = input.readBits(2);
            header.shadowFlags = input.readBits(8);
        }
        if (v.from(DwgVersion.R2010)) {
            header.hasFullVisualStyle = input.readBit();
            header.hasFaceVisualStyle = input.readBit();
            header.hasEdgeVisualStyle = input.readBit();
        }
        if (v.from(DwgVersion.R13)) {
            header.invisible = r.readBitShort();
        }
        if (v.from(DwgVersion.R2000)) {
            header.lineWeight = input.readBits(8);
        }
    }

    /**
     * ENC color of R2004+ (§2.11): a BS whose high byte holds flags. 0x20: a transparency BL
     * follows; 0x80: an RGB value (BL) follows; 0x40: the color has an AcDbColor handle.
     * Not in the specification, and not yet checked against a file (see TODO.md): the
     * transparency is read before the RGB value, there is no RGB value when 0x40 is set, and
     * R2004–R2007 files are read as having the color handle in the data stream.
     */
    private static void readEncColor(BitStreamReader r, DwgVersion v, ObjectHeaderData header) {
        int value = r.readBitShort();
        int flags = (value >> 8) & 0xFF;
        if (header != null) {
            header.colorIndex = value & 0x1FF;
            header.colorFlags = flags;
        }
        if ((flags & 0x20) != 0) {
            r.readBitLong();                       // transparency
        }
        if ((flags & 0x40) != 0) {
            if (!v.from(DwgVersion.R2010)) {
                readHandle(r.getInput());          // color handle
            }
        } else if ((flags & 0x80) != 0) {
            r.readBitLong();                       // RGB
        }
    }

    /**
     * Skips a handle reference (§2.13): a byte with the code (high nibble) and the number of
     * handle bytes (low nibble), then those bytes.
     */
    public static void readHandle(BitInput input) {
        int count = input.readBits(8) & 0x0F;
        for (int i = 0; i < count; i++) {
            input.readBits(8);
        }
    }

    /** Reads a raw double (RD): 8 bytes, least significant first. */
    public static double readRawDouble(BitInput input) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= (long) (input.readBits(8) & 0xFF) << (8 * i);
        }
        return Double.longBitsToDouble(bits);
    }

    /**
     * Skips the part of a multi-line ATTRIB or ATTDEF (R2018+) that holds an embedded MTEXT,
     * written from spec §20.4.4:
     * <ol>
     *   <li>all fields of an MTEXT entity, starting from the entity mode: the common entity data
     *       of §20.4.1 from Entmode on, then the MTEXT fields of §20.4.46;</li>
     *   <li>annotative data size (BS); if greater than zero, that many bytes and an unknown BS
     *       (the registered application handle listed with them is in the handle stream).</li>
     * </ol>
     * Handles (the MTEXT style, the registered application) are in the handle stream in R2018
     * files, so none is read here. Not yet checked against a file: none of the samples has a
     * multi-line attribute.
     */
    public static void skipMTextEmbedded(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();

        // common entity data, from Entmode on (§20.4.1)
        input.readBits(2);                 // entmode (BB)
        r.readBitLong();                   // number of reactors (BL)
        input.readBit();                   // XDic missing flag (B)
        input.readBit();                   // has DS binary data (B), R2013+
        readEncColor(r, v, null);          // color (ENC)
        r.readBitDouble();                 // linetype scale (BD)
        input.readBits(2);                 // linetype flags (BB)
        input.readBits(2);                 // plotstyle flags (BB)
        input.readBits(2);                 // material flags (BB)
        input.readBits(8);                 // shadow flags (RC)
        input.readBits(3);                 // full, face and edge visual style flags (B x3), R2010+
        r.readBitShort();                  // invisibility (BS)
        input.readBits(8);                 // lineweight (RC)

        // MTEXT fields (§20.4.46)
        r.read3BitDouble();                // insertion point (3BD)
        r.read3BitDouble();                // extrusion (3BD)
        r.read3BitDouble();                // x-axis direction (3BD)
        r.readBitDouble();                 // rectangle width (BD)
        r.readBitDouble();                 // rectangle height (BD)
        r.readBitDouble();                 // text height (BD)
        r.readBitShort();                  // attachment (BS)
        r.readBitShort();                  // drawing direction (BS)
        r.readBitDouble();                 // extents height (BD)
        r.readBitDouble();                 // extents width (BD)
        r.readVariableText();              // text (TV)
        r.readBitShort();                  // line spacing style (BS)
        r.readBitDouble();                 // line spacing factor (BD)
        input.readBit();                   // unknown bit (B)
        int background = r.readBitLong();  // background flags (BL)
        if ((background & 0x11) != 0) {    // 0x01, or 0x10 (text frame) in R2018
            r.readBitLong();               // background scale factor (BL)
            r.readCmColor();               // background color (CMC)
            r.readBitLong();               // background transparency (BL)
        }
        boolean notAnnotative = input.readBit();
        if (notAnnotative) {
            r.readBitShort();              // version (BS)
            input.readBit();               // default flag (B)
            r.readBitLong();               // attachment point (BL)
            r.read3BitDouble();            // x-axis direction (3BD)
            r.read3BitDouble();            // insertion point (3BD)
            r.readBitDouble();             // rectangle width (BD)
            r.readBitDouble();             // rectangle height (BD)
            r.readBitDouble();             // extents width (BD)
            r.readBitDouble();             // extents height (BD)
            int columnType = r.readBitShort();
            if (columnType != 0) {
                int heightCount = r.readBitLong();
                r.readBitDouble();         // column width (BD)
                r.readBitDouble();         // gutter (BD)
                boolean autoHeight = input.readBit();
                input.readBit();           // flow reversed (B)
                if (!autoHeight && columnType == 2) {
                    for (int i = 0; i < heightCount; i++) {
                        r.readBitDouble(); // column height (BD)
                    }
                }
            }
        }

        // annotative data (§20.4.4)
        int annotativeSize = r.readBitShort();
        if (annotativeSize > 0) {
            for (int i = 0; i < annotativeSize; i++) {
                input.readBits(8);
            }
            r.readBitShort();              // unknown (BS)
        }
    }

    // --- private helpers ---

    private static long readRawLong(BitInput input) {
        long b0 = input.readBits(8) & 0xFF;
        long b1 = input.readBits(8) & 0xFF;
        long b2 = input.readBits(8) & 0xFF;
        long b3 = input.readBits(8) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
