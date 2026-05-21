package io.dwg.sections.objects;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;

/**
 * Entity header reading utilities, verified against libredwg source.
 *
 * Per dwg_decode_entity() + common_entity_data.spec:
 *   1. bitsize (RL, no alignment) — R2000..R2007 only
 *   2. object handle (H, no alignment)
 *   3. EED loop (BS size → H handle → size bytes, until size=0)
 *   4. CommonEntityData fields
 *
 * All byte reads use readBits(8) — equivalent to libredwg bit_read_RC(),
 * which reads at the current bit position without byte alignment.
 */
public class EntityHeaderReader {

    /**
     * Read bitsize + object handle + EED loop.
     * Must be called immediately after typeCode is consumed by the parser.
     */
    public static void readEntityHeader(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();

        // bitsize (RL = 4 raw bytes, no alignment) — R2000 to R2007
        // R13/R14: bitsize appears inside common_entity_data after preview_exists
        // R2010+:  bitsize handled separately before this call
        if (v.from(DwgVersion.R2000) && !v.from(DwgVersion.R2010)) {
            readRawLong(input);
        }

        // object handle (H): first byte = code[7:4] + count[3:0], then count bytes
        readHandle(input);

        // EED loop: BS size → if >0: H appid-handle + size data bytes; repeat until 0
        // Per libredwg dwg_decode_eed(): size = bit_read_BS(dat)
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
     * Read CommonEntityData fields.
     * Must be called immediately after readEntityHeader().
     * Per libredwg common_entity_data.spec, field order and version guards verified.
     */
    public static void readCommonEntityData(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();

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
            readRawLong(input);
        }

        // 3. entmode (BB)
        input.readBits(2);

        // 4. num_reactors (BL)
        r.readBitLong();

        // 5. isbylayerlt (B) — R13/R14 only (ODA bug note in spec)
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2000)) {
            input.readBit();
        }

        // 6. is_xdic_missing (B) — R2004+
        if (v.from(DwgVersion.R2004)) {
            input.readBit();
        }

        // 7. nolinks (B) — R13 to R2002 inclusive (R2000 included)
        if (v.from(DwgVersion.R13) && !v.from(DwgVersion.R2004)) {
            input.readBit();
        }

        // 8. has_ds_data (B) — R2013+
        if (v.from(DwgVersion.R2013)) {
            input.readBit();
        }

        // 9. Color
        if (v.from(DwgVersion.R2004)) {
            // ENC: BS raw value, upper byte = flags. Order per libredwg common_entity_data.spec:
            // 0x20=alpha_raw (BL from dat), 0x40=handle (hdl_dat/dat), 0x80=rgb (BL, only if !0x40)
            int colorRaw = r.readBitShort();
            int flags = (colorRaw >> 8) & 0xFF;
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
            r.readBitShort();
        }

        // 10. ltype_scale (BD) — R13+
        if (v.from(DwgVersion.R13)) {
            r.readBitDouble();
        }

        // 11. ltype_flags (BB) + plotstyle_flags (BB) — R2000+
        if (v.from(DwgVersion.R2000)) {
            input.readBits(2);
            input.readBits(2);
        }

        // 12. material_flags (BB) + shadow_flags (RC) — R2007+
        if (v.from(DwgVersion.R2007)) {
            input.readBits(2);      // material_flags
            input.readBits(8);      // shadow_flags: RC = readBits(8), no alignment
        }

        // 13. has_full/face/edge_visualstyle (B x3) — R2010+
        if (v.from(DwgVersion.R2010)) {
            input.readBit();
            input.readBit();
            input.readBit();
        }

        // 14. invisible (BS) — R13+
        if (v.from(DwgVersion.R13)) {
            r.readBitShort();
        }

        // 15. linewt (RC) — R2000+
        // RC = readBits(8), no byte alignment per libredwg bit_read_RC()
        if (v.from(DwgVersion.R2000)) {
            input.readBits(8);
        }
    }

    /**
     * Read handle (H) without byte alignment.
     * Per libredwg bit_read_H(): first byte = code[7:4]+count[3:0], then count bytes.
     */
    public static void readHandle(BitInput input) {
        int firstByte = input.readBits(8) & 0xFF;
        int count = firstByte & 0x0F;
        for (int i = 0; i < count; i++) {
            input.readBits(8);
        }
    }

    /**
     * Read raw double (RD = 8 bytes, little-endian) without byte alignment.
     * Per libredwg bit_read_RD(): 8 x bit_read_RC().
     */
    public static double readRawDouble(BitInput input) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= (long)(input.readBits(8) & 0xFF) << (i * 8);
        }
        return Double.longBitsToDouble(bits);
    }

    /**
     * Skip embedded AcDbMTextObjectEmbedded subclass (R2018+).
     * Called when ATTRIB/ATTDEF mtext_type > 1 (multi-line attribute).
     * Fields from libredwg AcDbMTextObjectEmbedded_fields in dwg_spec_shared.h.
     */
    public static void skipMTextEmbedded(BitStreamReader r, DwgVersion v) throws Exception {
        BitInput input = r.getInput();
        // Common entity data portion of embedded MTEXT
        input.readBits(2);      // entmode (BB)
        r.readBitLong();        // num_reactors (BL)
        input.readBit();        // is_xdic_missing (B)
        input.readBit();        // has_ds_data (B)
        r.readBitShort();       // color.raw (BSx = BS)
        r.readBitDouble();      // ltype_scale (BD)
        input.readBits(2);      // ltype_flags (BB)
        input.readBits(2);      // plotstyle_flags (BB)
        input.readBits(2);      // material_flags (BB)
        input.readBits(8);      // shadow_flags (RC)
        input.readBit();        // has_full_visualstyle (B)
        input.readBit();        // has_face_visualstyle (B)
        input.readBit();        // has_edge_visualstyle (B)
        r.readBitShort();       // invisible (BS)
        input.readBits(8);      // linewt (RC)
        r.readHandle();         // layer handle

        // Embedded MTEXT object fields (non-DXF order)
        r.read3BitDouble();     // ins_pt (3BD)
        r.read3BitDouble();     // extrusion (3BD)
        r.read3BitDouble();     // x_axis_dir (3BD)
        r.readBitDouble();      // rect_width (BD)
        r.readBitDouble();      // rect_height (BD)
        r.readBitDouble();      // text_height (BD)
        r.readBitShort();       // attachment (BS)
        r.readBitShort();       // flow_dir (BS)
        r.readBitDouble();      // extents_height (BD)
        r.readBitDouble();      // extents_width (BD)
        r.readVariableText();   // text (T)
        r.readHandle();         // style handle (HANDLE0)
        r.readBitShort();       // linespace_style (BS)
        r.readBitDouble();      // linespace_factor (BD)
        input.readBit();        // unknown_b0 (B)
        int bgFillFlag = r.readBitLong(); // bg_fill_flag (BL0)
        // R2018: condition is (bgFillFlag & 1) per libredwg
        if ((bgFillFlag & 1) != 0) {
            r.readBitLong();    // bg_fill_scale (BL)
            r.readCmColor();    // bg_fill_color (CMC = BS+BL+RC for R2004+)
            r.readBitLong();    // bg_fill_trans (BL)
        }
        input.readBit();        // is_not_annotative (B)
        input.readBit();        // is_really_locked (B)

        // Non-DXF: annotative data
        int annotSize = r.readBitShort(); // annotative_data_size (BS)
        for (int i = 0; i < annotSize; i++) {
            input.readBits(8);  // annotative_data bytes (BINARY)
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
