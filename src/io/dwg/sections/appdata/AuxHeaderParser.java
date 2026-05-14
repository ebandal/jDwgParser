package io.dwg.sections.appdata;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

/**
 * AcDb:AuxHeader section parser - auxiliary header (R13+)
 * Spec §27: RC×2(sentinel) + various date/version fields
 */
public class AuxHeaderParser extends AbstractSectionParser<byte[]> {

    @Override
    public String sectionName() {
        return SectionType.AUX_HEADER.sectionName();
    }

    @Override
    public byte[] parse(SectionInputStream stream, DwgVersion version) throws Exception {
        BitStreamReader r = reader(stream, version);

        try {
            // RC×2 sentinel
            int sentinel1 = r.getInput().readRawChar() & 0xFF;
            int sentinel2 = r.getInput().readRawChar() & 0xFF;

            // Validate sentinel
            if (sentinel1 != 0xAC || sentinel2 != 0x10) {
                logUnknown("Invalid AuxHeader sentinel: " + String.format("%02X %02X", sentinel1, sentinel2), r.position());
            }

            // Read remaining data as-is for now
            return new byte[0]; // Placeholder - full parsing TBD
        } catch (Exception e) {
            logUnknown("AuxHeader parsing error: " + e.getMessage(), r.position());
            return new byte[0];
        }
    }
}
