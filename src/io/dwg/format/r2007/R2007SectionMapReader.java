package io.dwg.format.r2007;

import io.dwg.core.util.Lz77Decompressor;

/**
 * Read and decompress R2007 SectionMap (data page, not system page)
 */
public class R2007SectionMapReader {

    /**
     * Read and decompress SectionMap from data page
     * SectionMap is stored as a DATA PAGE (not a SYSTEM PAGE):
     * - NOT RS-encoded (only system pages are RS-encoded)
     * - IS LZ77-compressed (if sizeComp < sizeUncomp)
     * - Located at data offset calculated from PageMap
     */
    public static byte[] readSectionMapFromPage(byte[] pageData, long sizeComp, long sizeUncomp) throws Exception {
        if (sizeComp < 0 || sizeUncomp < 0) {
            throw new Exception("Invalid section map sizes");
        }

        if (pageData == null || pageData.length < sizeComp) {
            throw new Exception(String.format("Insufficient data: need %d bytes, have %d",
                sizeComp, pageData.length == 0 ? 0 : pageData.length));
        }

        // Data pages are LZ77-compressed if sizeComp < sizeUncomp
        if (sizeComp < sizeUncomp) {
            System.out.println("[DEBUG] SectionMap: LZ77 decompressing " + sizeComp +
                " bytes → " + sizeUncomp + " bytes");

            byte[] compressed = new byte[(int)sizeComp];
            System.arraycopy(pageData, 0, compressed, 0, (int)sizeComp);

            Lz77Decompressor lz77 = new Lz77Decompressor();
            return lz77.decompress(compressed, (int)(Math.min(sizeUncomp, Integer.MAX_VALUE)));
        } else {
            // Not compressed, use as-is
            byte[] result = new byte[(int)Math.min(sizeUncomp, Integer.MAX_VALUE)];
            System.arraycopy(pageData, 0, result, 0, result.length);
            return result;
        }
    }

}
