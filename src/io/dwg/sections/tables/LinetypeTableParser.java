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
 * @deprecated Linetype 테이블 파서 (Phase 4).
 *
 * 이 파서는 fake section name "AcDb:Linetypes"를 사용하며, handler.readSections()에서 절대 나타나지 않습니다.
 * 실제 LTYPE 객체들은 ObjectsSectionParser가 이미 파싱해 DwgDocument.objectMap에 저장합니다.
 *
 * 대신 다음 메서드를 사용하세요:
 * - DwgDocument.linetypes() - 모든 선종류 목록
 * - DwgDocument.linetype(String name) - 이름으로 선종류 검색
 * - DwgDocument.tables().linetypeByName(String name) - 테이블 로케이터 사용
 */
@Deprecated(since = "Phase 5", forRemoval = true)
public class LinetypeTableParser extends AbstractSectionParser<List<Map<String, Object>>> {

    @Override
    public String sectionName() {
        return "AcDb:Linetypes";
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;
    }

    @Override
    public List<Map<String, Object>> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        throw new UnsupportedOperationException(
            "LinetypeTableParser는 더 이상 지원되지 않습니다.\n" +
            "대신 DwgDocument.linetypes() 또는 DwgDocument.tables().linetypes()를 사용하세요.\n" +
            "ObjectsSectionParser가 이미 모든 LTYPE 객체를 파싱합니다."
        );
    }
}
