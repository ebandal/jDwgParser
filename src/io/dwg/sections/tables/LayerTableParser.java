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
 * @deprecated Layer 테이블 파서 (Phase 4).
 *
 * 이 파서는 fake section name "AcDb:Layers"를 사용하며, handler.readSections()에서 절대 나타나지 않습니다.
 * 실제 LAYER 객체들은 ObjectsSectionParser가 이미 파싱해 DwgDocument.objectMap에 저장합니다.
 *
 * 대신 다음 메서드를 사용하세요:
 * - DwgDocument.layers() - 모든 레이어 목록
 * - DwgDocument.layer(String name) - 이름으로 레이어 검색
 * - DwgDocument.tables().layerByName(String name) - 테이블 로케이터 사용
 */
@Deprecated(since = "Phase 5", forRemoval = true)
public class LayerTableParser extends AbstractSectionParser<List<DwgLayer>> {

    public LayerTableParser() {
    }

    @Override
    public String sectionName() {
        return "AcDb:Layers";
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;
    }

    @Override
    public List<DwgLayer> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        throw new UnsupportedOperationException(
            "LayerTableParser는 더 이상 지원되지 않습니다.\n" +
            "대신 DwgDocument.layers() 또는 DwgDocument.tables().layers()를 사용하세요.\n" +
            "ObjectsSectionParser가 이미 모든 LAYER 객체를 파싱합니다."
        );
    }
}
