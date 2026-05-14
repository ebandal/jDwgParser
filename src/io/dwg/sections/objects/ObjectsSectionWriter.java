package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.io.SectionOutputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.sections.SectionWriter;

import java.util.Map;

/**
 * Objects Section Writer (Spec §20: AcDb:AcDbObjects)
 * Serializes all DwgObject instances in handle order
 */
public class ObjectsSectionWriter implements SectionWriter<Map<Long, DwgObject>> {

    private final ObjectWriterRegistry writerRegistry;

    public ObjectsSectionWriter() {
        this.writerRegistry = ObjectWriterRegistry.defaultRegistry();
    }

    public ObjectsSectionWriter(ObjectWriterRegistry registry) {
        this.writerRegistry = registry;
    }

    @Override
    public String sectionName() {
        return "AcDb:AcDbObjects";
    }

    @Override
    public SectionOutputStream write(Map<Long, DwgObject> objectMap, DwgVersion version) throws Exception {
        SectionOutputStream section = new SectionOutputStream(sectionName());
        BitStreamWriter writer = section.writer(version);

        // Write objects in handle order (sorted)
        objectMap.entrySet().stream()
            .sorted((a, b) -> Long.compare(a.getKey(), b.getKey()))
            .forEach(entry -> {
                try {
                    writeObject(writer, entry.getValue(), version);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to write object handle " + entry.getKey(), e);
                }
            });

        return section;
    }

    private void writeObject(BitStreamWriter writer, DwgObject obj, DwgVersion version) throws Exception {
        // Placeholder for object size (MS - Modular Short)
        writer.writeModularShort(0);

        // Write type code (BS)
        writer.writeBitShort(obj.objectType().typeCode());

        // Write object-specific data using registered ObjectWriter
        ObjectWriter objWriter = writerRegistry.resolve(obj.objectType().typeCode())
            .orElseThrow(() -> new IllegalStateException(
                "No ObjectWriter registered for type " + obj.objectType().typeCode()));
        objWriter.write(obj, writer, version);

        // Note: In a production implementation, would need to go back and write actual size
        // This requires buffering or two-pass writing; for now using streaming approach
    }
}
