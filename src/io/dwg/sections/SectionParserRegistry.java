package io.dwg.sections;

import io.dwg.core.exception.UnknownSectionException;
import io.dwg.sections.appdata.AuxHeaderParser;
import io.dwg.sections.appdata.PreviewSectionParser;
import io.dwg.sections.appdata.SummaryInfoParser;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.header.HeaderSectionParser;
import io.dwg.sections.objects.ObjectsSectionParser;
import io.dwg.sections.tables.LayerTableParser;
import io.dwg.sections.tables.LinetypeTableParser;
import io.dwg.sections.tables.StyleTableParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 섹션 이름 → 파서 인스턴스 매핑 레지스트리.
 */
public class SectionParserRegistry {
    private final Map<String, SectionParser<?>> parsers = new HashMap<>();

    public void register(SectionParser<?> parser) {
        parsers.put(parser.sectionName(), parser);
    }

    public SectionParser<?> get(String sectionName) {
        SectionParser<?> p = parsers.get(sectionName);
        if (p == null) {
            throw new UnknownSectionException(sectionName);
        }
        return p;
    }

    public Optional<SectionParser<?>> find(String sectionName) {
        return Optional.ofNullable(parsers.get(sectionName));
    }

    public static SectionParserRegistry defaultRegistry() {
        SectionParserRegistry reg = new SectionParserRegistry();
        reg.register(new HeaderSectionParser());
        reg.register(new ClassesSectionParser());
        reg.register(new HandlesSectionParser());
        reg.register(new ObjectsSectionParser());
        reg.register(new SummaryInfoParser());
        reg.register(new PreviewSectionParser());
        reg.register(new AuxHeaderParser());

        // Auxiliary table parsers (Phase 4)
        reg.register(new LayerTableParser());
        reg.register(new LinetypeTableParser());
        reg.register(new StyleTableParser());

        return reg;
    }
}
