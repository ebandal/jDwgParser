package io.dwg.sections.tables;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.CmColor;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.sections.AbstractSectionParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer Table Parser - Extracts layer definitions from tables.
 *
 * Layers are typically stored as entries in the LAYER table object within
 * the Objects section. Each layer defines visibility, color, linetype, etc.
 */
public class LayerTableParser extends AbstractSectionParser<List<DwgLayer>> {

    public LayerTableParser() {
    }

    @Override
    public String sectionName() {
        return "AcDb:Layers";  // Pseudo-section for layers table
    }

    @Override
    public boolean supports(DwgVersion version) {
        // All DWG versions support layers
        return true;
    }

    @Override
    public List<DwgLayer> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        List<DwgLayer> layers = new ArrayList<>();

        if (stream == null || stream.rawBytes().length == 0) {
            System.out.printf("[DEBUG] Layers: Empty stream\n");
            return layers;
        }

        byte[] data = stream.rawBytes();
        BitStreamReader reader = reader(stream, version);

        System.out.printf("[DEBUG] Layers: Parsing %d bytes\n", data.length);

        // Layer table format depends on version
        // For now, implement basic structure that works across versions
        // Each layer entry contains: name (T), flags (BS), color (BS), linetype (BS), etc.

        int layerCount = 0;
        while (reader.position() < data.length * 8 - 16) {
            try {
                DwgLayer layer = parseOneLayer(reader, version);
                if (layer != null) {
                    layers.add(layer);
                    layerCount++;

                    if (layerCount <= 5 || layerCount % 50 == 0) {
                        System.out.printf("[DEBUG] Layers: Parsed layer %d\n", layerCount);
                    }
                }
            } catch (Exception e) {
                // Stop parsing on error
                System.out.printf("[DEBUG] Layers: Parse error at layer %d: %s\n", layerCount, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] Layers: Total parsed: %d\n", layerCount);
        return layers;
    }

    private DwgLayer parseOneLayer(BitStreamReader reader, DwgVersion version) throws Exception {
        // Layer structure (from spec):
        // - name: T (text string)
        // - flags: BS (bit short)
        // - color: BS (bit short) - color index (1-255, 0=by layer)
        // - linetype: BS (bit short) - linetype handle
        // - lineweight: BS or RC (varies by version)
        // - transparency: ? (varies by version)

        String name = reader.readText();  // Layer name
        if (name == null || name.isEmpty()) {
            return null;
        }

        int flags = reader.readBitShort();  // Frozen/off/on flags
        int colorIndex = reader.readBitShort();  // Color (1-255)
        int linetypeHandle = reader.readBitShort();  // Linetype handle

        DwgLayer layer = new DwgLayer();
        layer.setName(name);
        layer.setFlags(flags);

        // Parse flags into individual boolean properties
        boolean isFrozen = (flags & 0x01) != 0;
        boolean isOn = (flags & 0x02) == 0;  // Note: flag 0x02 means "off"
        layer.setFrozen(isFrozen);
        layer.setOn(isOn);

        // Set color (create simple color from index)
        if (colorIndex > 0) {
            CmColor color = new CmColor(colorIndex);
            layer.setColor(color);
        }

        // Set linetype handle
        if (linetypeHandle > 0) {
            DwgHandleRef ltHandle = new DwgHandleRef(linetypeHandle);
            layer.setLineTypeHandle(ltHandle);
        }

        // Optional: lineweight (varies by version)
        if (!version.until(DwgVersion.R14)) {
            try {
                int lineweight = reader.readBitShort();
                layer.setLineWeight(lineweight);
            } catch (Exception e) {
                // Lineweight might not be present
            }
        }

        return layer;
    }
}
