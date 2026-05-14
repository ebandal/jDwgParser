package io.dwg.sections.appdata;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

import java.util.HashMap;
import java.util.Map;

/**
 * AcDb:SummaryInfo section parser - document metadata (title, subject, author, keywords, comments)
 * Composed of TV (Text Variable) fields.
 */
public class SummaryInfoParser extends AbstractSectionParser<Map<String, String>> {

    @Override
    public String sectionName() {
        return SectionType.SUMMARY_INFO.sectionName();
    }

    @Override
    public Map<String, String> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<String, String> summary = new HashMap<>();
        BitStreamReader r = reader(stream, version);

        try {
            String title = r.readVariableText();
            if (title != null && !title.isEmpty()) {
                summary.put("Title", title);
            }

            String subject = r.readVariableText();
            if (subject != null && !subject.isEmpty()) {
                summary.put("Subject", subject);
            }

            String author = r.readVariableText();
            if (author != null && !author.isEmpty()) {
                summary.put("Author", author);
            }

            String keywords = r.readVariableText();
            if (keywords != null && !keywords.isEmpty()) {
                summary.put("Keywords", keywords);
            }

            String comments = r.readVariableText();
            if (comments != null && !comments.isEmpty()) {
                summary.put("Comments", comments);
            }
        } catch (Exception e) {
            logUnknown("SummaryInfo parsing error: " + e.getMessage(), r.position());
        }

        return summary;
    }
}
