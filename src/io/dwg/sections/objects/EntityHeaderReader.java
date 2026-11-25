package io.dwg.sections.objects;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;

/**
 * Reads the start of an entity or object, after its type code.
 *
 * <p>Follows the Open Design Specification for .dwg files, §20.4.1 "Common Entity Data" and
 * chapter 28 "Extended Entity Data":
 * <ol>
 *   <li>object size in bits (RL) — R2000 to R2007;</li>
 *   <li>the object's handle (H);</li>
 *   <li>EED: repeated (BS length, application handle, length bytes) until a length of 0;</li>
 *   <li>the common entity data fields.</li>
 * </ol>
 * The object data is a bit stream: raw bytes are read at the current bit position, without
 * byte alignment.
 */
public class EntityHeaderReader {

    /**
     * Read bitsize + object handle + EED loop.
     * Must be called immediately after typeCode is consumed by the parser.
     * The values needed later for the handle stream are kept in {@link ObjectHeaderData}.
     */
    public static void readEntityHeader(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);

        // bitsize (RL = 4 raw bytes, no alignment) — R2000 to R2007
        // R13/R14: bitsize appears inside common_entity_data after preview_exists
        // R2010+:  bitsize handled separately before this call
        if (v.from(DwgVersion.R2000) && !v.from(DwgVersion.R2010)) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }

        // object handle (H): first byte = code[7:4] + count[3:0], then count bytes
        readHandle(input);

        // EED (spec chapter 28): BS length, application handle, length bytes; until length 0
        while (true) {
            int eedSize = r.readBitShort();
            if (eedSize == 0) break;
            if (eedSize > 0 && eedSize <= 0x7FFF) {
                readHandle(input);
                for (int i = 0; i < eedSize; i++) {
                    input.readBits(8);
                }
            } else {
                break;
            }
        }
    }

    /**
     * Read the common header of a non-entity object : bitsize + object handle + EED loop,
     * then the reactor count and the flags that precede the object data.
     * Must be called immediately after typeCode is consumed by the parser.
     * Verified on R2000 files; the later fields follow the specification.
     */
    public static void readObjectHeader(BitStreamReader r, DwgVersion v) throws Exception {
        readEntityHeader(r, v);
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);

        // R13/R14 only: bitsize (RL) comes after the EED
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000)) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }
        header.numReactors = r.readBitLong();           // num_reactors (BL)
        if (v.from(DwgVersion.R2004)) {
            header.xdicMissing = input.readBit();       // is_xdic_missing (B)
        }
        if (v.from(DwgVersion.R2013)) {
            input.readBit();                            // has_ds_data (B)
        }
    }

    /**
     * Read the flags that follow the name of a table record (LAYER, BLOCK_HEADER, ...).
     * Up to R2004 : 64-flag (B), xref index + 1 (BS), xdep (B). R2007+ : only the BS, where 256 means xdep.
     */
    public static void readTableRecordFlags(BitStreamReader r, DwgVersion v) throws Exception {
        if (v.from(DwgVersion.R2007)) {
            r.readBitShort();
        } else {
            r.getInput().readBit();
            r.readBitShort();
            r.getInput().readBit();
        }
    }

    /**
     * Read CommonEntityData fields.
     * Must be called immediately after readEntityHeader().
     * Field order of spec §20.4.1, with two points the table there leaves open, both settled by
     * the sample files (R2004+ files render like their R2000 counterparts only this way):
     * <ul>
     *   <li>Nolinks is present only up to R2000; R2004+ files have no such bit.</li>
     *   <li>R2010+ entities have three visual style bits before the invisibility (the
     *       handle data of §20.4.2 refers to them).</li>
     * </ul>
     * The values are kept in {@link ObjectHeaderData}.
     */
    public static void readCommonEntityData(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();
        ObjectHeaderData header = ObjectHeaderData.of(r);
        header.entity = true;

        // 1. preview_exists (B)
        boolean previewExists = input.readBit();
        if (previewExists) {
            // preview_size: RL for R13-R2007, BLL for R2010+
            long previewSize;
            if (v.from(DwgVersion.R2010)) {
                previewSize = r.readBitLongLong();
            } else {
                previewSize = readRawLong(input);
            }
            // O(1) seek past preview bytes; invalid size throws and fails entity gracefully
            r.seek(r.position() + previewSize * 8L);
        }

        // 2. R13/R14 only: bitsize (RL) appears here, after preview_exists
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000)) {
            header.bitSize = readRawLong(input) & 0xFFFFFFFFL;
        }

        // 3. entmode (BB)
        header.entMode = input.readBits(2);

        // 4. num_reactors (BL)
        header.numReactors = r.readBitLong();

        // 5. isbylayerlt (B) — R13/R14 only (ODA bug note in spec)
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000)) {
            header.isByLayerLinetype = input.readBit();
        }

        // 6. is_xdic_missing (B) — R2004+
        if (v.from(DwgVersion.R2004)) {
            header.xdicMissing = input.readBit();
        }

        // 7. nolinks (B) — R13 to R2002 inclusive (R2000 included)
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2004)) {
            header.noLinks = input.readBit();
        }

        // 8. has_ds_data (B) — R2013+
        if (v.from(DwgVersion.R2013)) {
            input.readBit();
        }

        // 9. Color
        if (v.from(DwgVersion.R2004)) {
            // ENC (spec §2.11): BS whose high byte holds flags. 0x20: a transparency BL follows;
            // 0x80: an RGB value (BL) follows; 0x40: the color has an AcDbColor handle.
            // Not in the spec, and not yet checked against a file (see TODO.md): the
            // transparency is read before the RGB value, and R2004–R2007 files are read as
            // having the color handle in the data stream rather than the handle stream.
            int colorRaw = r.readBitShort();
            int flags = (colorRaw >> 8) & 0xFF;
            header.colorIndex = colorRaw & 0x1FF;
            header.colorFlags = flags;
            if ((flags & 0x20) != 0) r.readBitLong();      // color.alpha_raw (BL) — first
            if ((flags & 0x40) != 0) {
                if (!v.from(DwgVersion.R2010)) {
                    readHandle(input);  // color.handle from dat (R2004-R2007 only)
                }
                // R2010+: color.handle lives in hdl_dat — not in main stream
            } else if ((flags & 0x80) != 0) {
                r.readBitLong();        // color.rgb (BL) — only when no color handle
            }
        } else if (v.from(DwgVersion.R13)) {
            // CMC for R13-R2003: just a BS color index
            header.colorIndex = r.readBitShort();
        }

        // 10. ltype_scale (BD) — R13+
        if (v.from(DwgVersion.R13)) {
            header.linetypeScale = r.readBitDouble();
        }

        // 11. ltype_flags (BB) + plotstyle_flags (BB) — R2000+
        if (v.from(DwgVersion.R2000)) {
            header.linetypeFlags = input.readBits(2);
            header.plotStyleFlags = input.readBits(2);
        }

        // 12. material_flags (BB) + shadow_flags (RC) — R2007+
        if (v.from(DwgVersion.R2007)) {
            header.materialFlags = input.readBits(2);
            header.shadowFlags = input.readBits(8);     // RC = readBits(8), no alignment
        }

        // 13. has_full/face/edge_visualstyle (B x3) — R2010+
        if (v.from(DwgVersion.R2010)) {
            header.hasFullVisualStyle = input.readBit();
            header.hasFaceVisualStyle = input.readBit();
            header.hasEdgeVisualStyle = input.readBit();
        }

        // 14. invisible (BS) — R13+
        if (v.from(DwgVersion.R13)) {
            header.invisible = r.readBitShort();
        }

        // 15. linewt (RC) — R2000+
        if (v.from(DwgVersion.R2000)) {
            header.lineWeight = input.readBits(8);
        }
    }

    /**
     * Skips a handle reference (spec §2.13): a byte holding the code (high nibble) and the
     * number of handle bytes (low nibble), then that many bytes.
     */
    public static void readHandle(BitInput input) {
        int firstByte = input.readBits(8) & 0xFF;
        int count = firstByte & 0x0F;
        for (int i = 0; i < count; i++) {
            input.readBits(8);
        }
    }

    /**
     * Reads a raw double (RD): 8 bytes, least significant first, at the current bit position.
     */
    public static double readRawDouble(BitInput input) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= (long)(input.readBits(8) & 0xFF) << (i * 8);
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
        skipEncColor(r);                   // color (ENC)
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

    /** Skips an ENC color (spec §2.11) in the same way as {@link #readCommonEntityData}. */
    private static void skipEncColor(BitStreamReader r) {
        int flags = (r.readBitShort() >> 8) & 0xFF;
        if ((flags & 0x20) != 0) r.readBitLong();
        if ((flags & 0x40) == 0 && (flags & 0x80) != 0) r.readBitLong();
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
