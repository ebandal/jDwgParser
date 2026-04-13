package io.dwg.core.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 문자열 디코딩 유틸리티
 */
public class DwgStringDecoder {
    
    /**
     * DWG 버전에 맞게 문자열 디코딩
     */
    public static String decode(byte[] bytes, DwgVersion ver) {
        if (ver.usesUnicode()) {
            return decodeUtf16Le(bytes);
        } else {
            return decodeCodePage(bytes, 20127);  // 기본값: US-ASCII
        }
    }

    /**
     * 지정 코드페이지로 디코딩
     */
    public static String decodeCodePage(byte[] bytes, int codePage) {
        Charset charset = charsetForCodePage(codePage);
        if (charset != null) {
            return new String(bytes, charset);
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * UTF-16LE로 디코딩
     */
    public static String decodeUtf16Le(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_16LE);
    }

    /**
     * DWG 코드페이지 번호 → Java Charset 매핑
     */
    public static Charset charsetForCodePage(int codePage) {
        try {
            return switch (codePage) {
                case 20127, 20000 -> StandardCharsets.US_ASCII;
                case 20932 -> Charset.forName("Shift_JIS");
                case 20936 -> Charset.forName("GB2312");
                case 20949 -> Charset.forName("EUC-KR");
                case 1252 -> StandardCharsets.ISO_8859_1;
                case 1250 -> Charset.forName("ISO-8859-2");
                case 1251 -> Charset.forName("ISO-8859-5");
                case 1253 -> Charset.forName("ISO-8859-7");
                case 1254 -> Charset.forName("ISO-8859-9");
                case 1255 -> Charset.forName("ISO-8859-8");
                case 1256 -> Charset.forName("ISO-8859-6");
                case 1257 -> Charset.forName("ISO-8859-13");
                default -> StandardCharsets.ISO_8859_1;
            };
        } catch (Exception e) {
            return StandardCharsets.ISO_8859_1;
        }
    }
}
