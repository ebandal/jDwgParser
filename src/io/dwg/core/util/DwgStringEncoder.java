package io.dwg.core.util;

import java.nio.charset.StandardCharsets;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 문자열 인코딩 유틸리티
 */
public class DwgStringEncoder {
    
    /**
     * 버전에 맞는 인코딩으로 변환
     */
    public static byte[] encode(String text, DwgVersion ver) {
        if (ver.usesUnicode()) {
            return encodeUtf16Le(text);
        } else {
            return text.getBytes(StandardCharsets.US_ASCII);
        }
    }

    /**
     * UTF-16LE로 변환
     */
    public static byte[] encodeUtf16Le(String text) {
        return text.getBytes(StandardCharsets.UTF_16LE);
    }
}
