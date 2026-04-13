package io.dwg.sections;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;

/**
 * 공통 편의 메서드 제공 기반 클래스.
 */
public abstract class AbstractSectionParser<T> implements SectionParser<T> {

    protected BitStreamReader reader(SectionInputStream s, DwgVersion v) {
        return s.reader(v);
    }

    protected void skipBytes(BitStreamReader r, int count) {
        for (int i = 0; i < count; i++) {
            r.getInput().readRawChar();
        }
    }

    protected void validateSentinel(BitStreamReader r, byte[] expected) {
        for (byte b : expected) {
            int actual = r.getInput().readRawChar();
            if ((actual & 0xFF) != (b & 0xFF)) {
                // sentinel 불일치는 경고만 출력 (파싱 계속)
                logUnknown("sentinel mismatch", r.position());
                return;
            }
        }
    }

    protected void logUnknown(String field, long bitPos) {
        // 필요 시 로거 연결
        System.err.println("[WARN] unknown field: " + field + " at bit " + bitPos);
    }

    @Override
    public boolean supports(DwgVersion version) {
        return true;
    }
}
