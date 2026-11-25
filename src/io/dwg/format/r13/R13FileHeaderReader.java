package io.dwg.format.r13;

import io.dwg.core.io.BitInput;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reads the file header of R13, R14 and R2000 files (spec §3.2).
 *
 * <pre>
 *   0x00  6  version id, e.g. "AC1015"
 *   0x06  7  six zero bytes (R14: five, then ACADMAINTVER) and a byte of 1 (§3.2.1)
 *   0x0D  4  image seeker (§3.2.2)
 *   0x11  2  application version and maintenance version
 *   0x13  2  code page (§3.2.5)
 *   0x15  4  number of section-locator records, then the records (§3.2.6):
 *            record number (raw byte), seeker (raw long), size (raw long)
 *         2  CRC
 * </pre>
 */
public final class R13FileHeaderReader {

    /** The values of the file header. */
    public static final class Header {
        public final String versionId;
        public final long imageSeeker;
        public final int maintenanceVersion;
        public final int codePage;
        public final List<R13SectionLocator> locators;

        Header(String versionId, long imageSeeker, int maintenanceVersion, int codePage,
               List<R13SectionLocator> locators) {
            this.versionId = versionId;
            this.imageSeeker = imageSeeker;
            this.maintenanceVersion = maintenanceVersion;
            this.codePage = codePage;
            this.locators = locators;
        }
    }

    private R13FileHeaderReader() {
    }

    /**
     * Reads the header from the start of the file.
     *
     * @param acceptVersion tells whether the version id belongs to the caller's version
     * @param label         the version named in the exception
     * @throws IllegalArgumentException if the version id is not accepted; nothing after it is read
     */
    public static Header read(BitInput input, Predicate<String> acceptVersion, String label) {
        byte[] id = new byte[6];
        for (int i = 0; i < id.length; i++) {
            id[i] = (byte) input.readRawChar();
        }
        String versionId = new String(id, StandardCharsets.US_ASCII);
        if (!acceptVersion.test(versionId)) {
            throw new IllegalArgumentException("Invalid " + label + " version string: " + versionId);
        }

        skipBytes(input, 7);                                  // 0x06–0x0C
        long imageSeeker = input.readRawLong() & 0xFFFFFFFFL; // 0x0D
        input.readRawChar();                                  // 0x11 application version
        int maintenanceVersion = input.readRawChar();         // 0x12
        int codePage = input.readRawShort() & 0xFFFF;         // 0x13

        int recordCount = input.readRawLong();                // 0x15
        List<R13SectionLocator> locators = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            locators.add(R13SectionLocator.read(input));
        }
        input.readRawShort();                                 // CRC

        return new Header(versionId, imageSeeker, maintenanceVersion, codePage, locators);
    }

    private static void skipBytes(BitInput input, int count) {
        for (int i = 0; i < count; i++) {
            input.readRawChar();
        }
    }
}
