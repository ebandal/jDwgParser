package io.dwg.sections.appdata;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

/**
 * AcDb:Preview section parser - thumbnail image data
 * Spec §14: Image count(RL) -> for each image: type(RC) + size(RL) + data(RC×size)
 */
public class PreviewSectionParser extends AbstractSectionParser<byte[]> {

    @Override
    public String sectionName() {
        return SectionType.PREVIEW.sectionName();
    }

    @Override
    public byte[] parse(SectionInputStream stream, DwgVersion version) throws Exception {
        BitStreamReader r = reader(stream, version);

        try {
            // Image count (RL) - for now, read but don't process
            r.getInput().readRawLong();

            // For now, read remaining data as-is
            return new byte[0]; // Placeholder - full parsing TBD
        } catch (Exception e) {
            logUnknown("Preview parsing error: " + e.getMessage(), r.position());
            return new byte[0];
        }
    }
}
