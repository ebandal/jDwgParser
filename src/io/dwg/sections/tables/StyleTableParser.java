package io.dwg.sections.tables;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.concrete.DwgStyle;
import io.dwg.sections.AbstractSectionParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Text Style Table Parser - Extracts text style definitions from tables.
 *
 * Text styles define font, height, width factor, oblique angle, and other
 * text formatting properties that can be applied to text and attributes.
 */
public class StyleTableParser extends AbstractSectionParser<List<DwgStyle>> {

    @Override
    public String sectionName() {
        return "AcDb:Styles";  // Pseudo-section for text styles
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;  // All versions support text styles
    }

    @Override
    public List<DwgStyle> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        List<DwgStyle> styles = new ArrayList<>();

        if (stream == null || stream.rawBytes().length == 0) {
            System.out.printf("[DEBUG] Styles: Empty stream\n");
            return styles;
        }

        byte[] data = stream.rawBytes();
        BitStreamReader reader = reader(stream, version);

        System.out.printf("[DEBUG] Styles: Parsing %d bytes\n", data.length);

        int styleCount = 0;
        while (reader.position() < data.length * 8 - 16) {
            try {
                DwgStyle style = parseOneStyle(reader, version);
                if (style != null) {
                    styles.add(style);
                    styleCount++;

                    if (styleCount <= 5 || styleCount % 50 == 0) {
                        System.out.printf("[DEBUG] Styles: Parsed style %d\n", styleCount);
                    }
                }
            } catch (Exception e) {
                System.out.printf("[DEBUG] Styles: Parse error at style %d: %s\n",
                    styleCount, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] Styles: Total parsed: %d\n", styleCount);
        return styles;
    }

    private DwgStyle parseOneStyle(BitStreamReader reader, DwgVersion version) throws Exception {
        // Text style structure (from spec):
        // - name: T (text string)
        // - flags: BS (bit short)
        // - fixed_text_height: BD (double) - height in units
        // - width_factor: BD (double) - width scaling
        // - oblique_angle: BD (double) - slant in degrees
        // - generation_flags: BS (bit short) - mirroring/backwards
        // - font_name: T (primary font name)
        // - big_font_name: T (shape file name or CJK font)

        String name = reader.readText();
        if (name == null || name.isEmpty()) {
            return null;
        }

        int flags = reader.readBitShort();
        double fixedHeight = reader.readBitDouble();
        double widthFactor = reader.readBitDouble();
        double obliqueAngle = reader.readBitDouble();
        int genFlags = reader.readBitShort();

        String fontName = reader.readText();
        String bigFontName = reader.readText();

        DwgStyle style = new DwgStyle();
        style.setName(name);
        style.setFlags(flags);
        style.setWidth(widthFactor);  // Width scaling factor
        style.setOblique(obliqueAngle);  // Oblique angle in degrees

        if (fontName != null && !fontName.isEmpty()) {
            style.setFontFilename(fontName);
        }

        if (bigFontName != null && !bigFontName.isEmpty()) {
            style.setBigFontFilename(bigFontName);
        }

        return style;
    }
}
