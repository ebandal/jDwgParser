package io.dwg.sections.objects.readers;

import io.dwg.core.type.CmColor;

/** Turns the values of a CMC (§2.11, as {@link io.dwg.core.io.BitStreamReader#readCmColor}) into a color. */
final class Colors {

    /** Color methods in the high byte of the R2004+ RGB value. */
    private static final int BY_LAYER = 0xC0;
    private static final int BY_BLOCK = 0xC1;
    private static final int BY_INDEX = 0xC3;

    private static final int INDEX_BY_BLOCK = 0;
    private static final int INDEX_BY_LAYER = 256;

    private Colors() {
    }

    /**
     * Up to R2000 the CMC is the color index. In R2004+ the index field is not used: the high
     * byte of the RGB value says how the color is given. Measured against the R2000 copies of
     * the sample drawings: 0xC0 is ByLayer, 0xC1 ByBlock, and with 0xC3 the low byte is the
     * color index. Other methods (true colors; none in the samples) keep the index field. The
     * method and RGB value are kept in the color.
     */
    static CmColor of(int[] cmc) {
        if (cmc.length == 1) {
            return new CmColor(cmc[0]);
        }
        int method = (cmc[1] >>> 24) & 0xFF;
        int index;
        switch (method) {
            case BY_LAYER: index = INDEX_BY_LAYER; break;
            case BY_BLOCK: index = INDEX_BY_BLOCK; break;
            case BY_INDEX: index = cmc[1] & 0xFF; break;
            default:       index = cmc[0];
        }
        return new CmColor(index, cmc[1] & 0xFFFFFF, (byte) method);
    }
}
