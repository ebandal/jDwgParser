package io.dwg.render.r2000;

import java.nio.charset.Charset;

import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.DwgObject;
import io.dwg.render.model.Layer;

/** Decodes R2000 (AC1015) table records needed for rendering : BLOCK_HEADER and LAYER. */
public class TableDecoderR2000 {
    public static final int TYPE_BLOCK_HEADER = 0x31;
    public static final int TYPE_LAYER = 0x33;

    public static BlockHeader decodeBlockHeader(byte[] file, DwgObject obj, Charset charset) throws DwgParseException {
        try {
            BitReader r = start(file, obj, charset);
            BlockHeader b = new BlockHeader();
            b.handle = obj.handle;
            b.name = r.readTV();
            r.readBit();                        // 64-flag
            r.readBS();                         // xref index + 1
            r.readBit();                        // xdep
            b.anonymous = r.readBit();
            r.readBit();                        // has attributes
            b.xref = r.readBit();
            b.xref |= r.readBit();              // xref overlaid
            r.readBit();                        // loaded
            b.basePoint = r.read3BD();
            r.readTV();                         // xref path name
            while (r.readRC() != 0) {           // insert count
            }
            r.readTV();                         // description
            int previewSize = r.readBL();
            r.seekBit(r.bitPosition() + 8L * previewSize);
            checkEnd(r, obj);
            return b;
        } catch (RuntimeException ex) {
            throw new DwgParseException();
        }
    }

    public static Layer decodeLayer(byte[] file, DwgObject obj, Charset charset) throws DwgParseException {
        try {
            BitReader r = start(file, obj, charset);
            Layer l = new Layer();
            l.handle = obj.handle;
            l.name = r.readTV();
            r.readBit();                        // 64-flag
            r.readBS();                         // xref index + 1
            r.readBit();                        // xdep
            int flags = r.readBS();             // 1 frozen, 2 off, 4 frozen in new viewports, 8 locked, ...
            l.frozen = (flags & 1) != 0;
            l.off = (flags & 2) != 0;
            l.color = r.readBS();
            checkEnd(r, obj);
            return l;
        } catch (RuntimeException ex) {
            throw new DwgParseException();
        }
    }

    // skips the common part of a non-entity object : extended data and the reactor count
    private static BitReader start(byte[] file, DwgObject obj, Charset charset) {
        BitReader r = new BitReader(file, 0).charset(charset);
        r.seekBit(obj.bodyBitPos);
        while (true) {
            int size = r.readBS();
            if (size == 0) {
                break;
            }
            r.readHandle();
            r.seekBit(r.bitPosition() + 8L * size);
        }
        r.readBL();                             // number of reactors
        return r;
    }

    // the object data must end where the handle stream starts
    private static void checkEnd(BitReader r, DwgObject obj) {
        if (r.bitPosition() != (long) obj.dataOffset * 8 + obj.bitSize) {
            throw new IllegalStateException("object data does not end at the handle stream");
        }
    }
}
