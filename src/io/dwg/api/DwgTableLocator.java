package io.dwg.api;

import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.entities.concrete.DwgStyle;

import java.util.List;
import java.util.Optional;

/**
 * DWG 파일의 테이블 객체(레이어, 선종류, 스타일 등) 접근 API.
 * DwgDocument의 objectMap에서 해당 타입의 객체들을 필터링해 제공.
 */
public class DwgTableLocator {
    private final DwgDocument doc;

    public DwgTableLocator(DwgDocument doc) {
        this.doc = doc;
    }

    // ========== Layers ==========

    /** 모든 레이어 목록 */
    public List<DwgLayer> layers() {
        return doc.layers();
    }

    /** 이름으로 레이어 검색 */
    public Optional<DwgLayer> layerByName(String name) {
        return doc.layer(name);
    }

    // ========== Linetypes ==========

    /** 모든 선종류 목록 */
    public List<DwgLtype> linetypes() {
        return doc.linetypes();
    }

    /** 이름으로 선종류 검색 */
    public Optional<DwgLtype> linetypeByName(String name) {
        return doc.linetype(name);
    }

    // ========== Styles ==========

    /** 모든 텍스트 스타일 목록 */
    public List<DwgStyle> styles() {
        return doc.styles();
    }

    /** 이름으로 텍스트 스타일 검색 */
    public Optional<DwgStyle> styleByName(String name) {
        return doc.style(name);
    }

    // ========== Generic ==========

    /** 지정 타입의 모든 객체 검색 (다른 테이블 타입 접근용) */
    public <T extends DwgObject> List<T> table(Class<T> type) {
        return doc.objectsOfType(type);
    }
}
