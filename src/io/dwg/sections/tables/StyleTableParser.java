package io.dwg.sections.tables;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.concrete.DwgStyle;
import io.dwg.sections.AbstractSectionParser;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Text Style 테이블 파서 (Phase 4).
 *
 * 이 파서는 fake section name "AcDb:Styles"를 사용하며, handler.readSections()에서 절대 나타나지 않습니다.
 * 실제 STYLE 객체들은 ObjectsSectionParser가 이미 파싱해 DwgDocument.objectMap에 저장합니다.
 *
 * 대신 다음 메서드를 사용하세요:
 * - DwgDocument.styles() - 모든 텍스트 스타일 목록
 * - DwgDocument.style(String name) - 이름으로 스타일 검색
 * - DwgDocument.tables().styleByName(String name) - 테이블 로케이터 사용
 */
@Deprecated(since = "Phase 5", forRemoval = true)
public class StyleTableParser extends AbstractSectionParser<List<DwgStyle>> {

    @Override
    public String sectionName() {
        return "AcDb:Styles";
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;
    }

    @Override
    public List<DwgStyle> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        throw new UnsupportedOperationException(
            "StyleTableParser는 더 이상 지원되지 않습니다.\n" +
            "대신 DwgDocument.styles() 또는 DwgDocument.tables().styles()를 사용하세요.\n" +
            "ObjectsSectionParser가 이미 모든 STYLE 객체를 파싱합니다."
        );
    }
}
