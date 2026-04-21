package io.dwg.sections.tables;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.sections.AbstractSectionParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Linetype Table Parser - Extracts linetype definitions from tables.
 *
 * Linetypes define the dash/dot pattern for lines, arcs, circles, etc.
 * Each linetype has a name, description, pattern, and scaling information.
 */
public class LinetypeTableParser extends AbstractSectionParser<List<Map<String, Object>>> {

    @Override
    public String sectionName() {
        return "AcDb:Linetypes";  // Pseudo-section for linetypes table
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;  // All versions support linetypes
    }

    @Override
    public List<Map<String, Object>> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        List<Map<String, Object>> linetypes = new ArrayList<>();

        if (stream == null || stream.rawBytes().length == 0) {
            System.out.printf("[DEBUG] Linetypes: Empty stream\n");
            return linetypes;
        }

        byte[] data = stream.rawBytes();
        BitStreamReader reader = reader(stream, version);

        System.out.printf("[DEBUG] Linetypes: Parsing %d bytes\n", data.length);

        int linetypeCount = 0;
        while (reader.position() < data.length * 8 - 16) {
            try {
                Map<String, Object> lt = parseOneLinetype(reader, version);
                if (lt != null) {
                    linetypes.add(lt);
                    linetypeCount++;

                    if (linetypeCount <= 5 || linetypeCount % 50 == 0) {
                        System.out.printf("[DEBUG] Linetypes: Parsed linetype %d (%s)\n",
                            linetypeCount, lt.get("name"));
                    }
                }
            } catch (Exception e) {
                System.out.printf("[DEBUG] Linetypes: Parse error at linetype %d: %s\n",
                    linetypeCount, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] Linetypes: Total parsed: %d\n", linetypeCount);
        return linetypes;
    }

    private Map<String, Object> parseOneLinetype(BitStreamReader reader, DwgVersion version) throws Exception {
        // Linetype structure (from spec):
        // - name: T (text string)
        // - description: T (descriptive string)
        // - flags: BS (bit short)
        // - num_dashes: BS (number of dash segments)
        // - total_pattern_length: BD (double)
        // - dashes: array of BDs

        String name = reader.readText();
        if (name == null || name.isEmpty()) {
            return null;
        }

        String description = reader.readText();
        int flags = reader.readBitShort();
        int numDashes = reader.readBitShort();
        double totalLength = reader.readBitDouble();

        Map<String, Object> lt = new HashMap<>();
        lt.put("name", name);
        lt.put("description", description != null ? description : "");
        lt.put("flags", flags);
        lt.put("dashCount", numDashes);
        lt.put("totalLength", totalLength);

        // Read dashes array
        List<Double> dashes = new ArrayList<>();
        for (int i = 0; i < numDashes && i < 100; i++) {  // Safety limit
            try {
                double dash = reader.readBitDouble();
                dashes.add(dash);
            } catch (Exception e) {
                break;
            }
        }
        lt.put("dashes", dashes);

        return lt;
    }
}
