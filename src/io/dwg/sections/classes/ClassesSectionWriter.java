package io.dwg.sections.classes;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.io.SectionOutputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.SectionWriter;

import java.util.List;

/**
 * Classes Section Writer (Spec §10: AcDb:Classes)
 * Serializes class definitions for custom entity types
 */
public class ClassesSectionWriter implements SectionWriter<List<DwgClassDefinition>> {

    static final byte[] START_SENTINEL = {
        (byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
        (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
        (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A
    };

    static final byte[] END_SENTINEL = {
        (byte)0x72, (byte)0x5E, (byte)0x3B, (byte)0x47, (byte)0x3B, (byte)0x56,
        (byte)0x07, (byte)0x3A, (byte)0x3F, (byte)0x23, (byte)0x0B, (byte)0xA0,
        (byte)0x18, (byte)0x30, (byte)0x49, (byte)0x75
    };

    @Override
    public String sectionName() {
        return SectionType.CLASSES.sectionName();
    }

    @Override
    public SectionOutputStream write(List<DwgClassDefinition> classes, DwgVersion version) throws Exception {
        SectionOutputStream section = new SectionOutputStream(sectionName());
        BitStreamWriter writer = section.writer(version);

        // Write start sentinel
        for (byte b : START_SENTINEL) {
            writer.getOutput().writeRawChar(b & 0xFF);
        }

        // Calculate and write section size (placeholder for now)
        // In a production implementation, would need to buffer content first
        writer.getOutput().writeRawLong(0);

        // Write each class definition
        for (DwgClassDefinition def : classes) {
            writeClassDefinition(writer, def);
        }

        // Write end sentinel
        for (byte b : END_SENTINEL) {
            writer.getOutput().writeRawChar(b & 0xFF);
        }

        return section;
    }

    private void writeClassDefinition(BitStreamWriter writer, DwgClassDefinition def) throws Exception {
        writer.writeBitShort(def.classNumber());
        writer.writeBitShort(def.version());
        writer.writeVariableText(def.applicationName());
        writer.writeVariableText(def.cppClassName());
        writer.writeVariableText(def.dxfRecordName());
        writer.getOutput().writeBit(def.wasAZombie());
        writer.writeBitShort(def.isEntity() ? 1 : 0);
    }
}
