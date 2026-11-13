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

    /**
     * DWGCODEPAGE 인덱스(파일 헤더 0x13의 값) → Java Charset.
     * {@link #charsetForCodePage(int)}가 받는 Windows 코드페이지 번호와는 다른 번호 체계이다.
     * 모르는 인덱스나 이 JVM에 없는 문자 집합은 windows-1252로 처리한다.
     */
    public static Charset charsetForDwgCodePage(int codePage) {
        String name;
        switch (codePage) {
        case 1:             name = "US-ASCII";      break;
        case 2:             name = "ISO-8859-1";    break;
        case 3:             name = "ISO-8859-2";    break;
        case 4:             name = "ISO-8859-3";    break;
        case 5:             name = "ISO-8859-4";    break;
        case 6:             name = "ISO-8859-5";    break;
        case 7:             name = "ISO-8859-6";    break;
        case 8:             name = "ISO-8859-7";    break;
        case 9:             name = "ISO-8859-8";    break;
        case 10:            name = "ISO-8859-9";    break;
        case 11:            name = "IBM437";        break;
        case 12:            name = "IBM850";        break;
        case 13:            name = "IBM852";        break;
        case 14:            name = "IBM855";        break;
        case 15:            name = "IBM857";        break;
        case 16:            name = "IBM860";        break;
        case 17:            name = "IBM861";        break;
        case 18:            name = "IBM863";        break;
        case 19:            name = "IBM864";        break;
        case 20:            name = "IBM865";        break;
        case 21:            name = "IBM869";        break;
        case 22: case 38:   name = "windows-31j";   break;
        case 23:            name = "x-MacRoman";    break;
        case 24: case 41:   name = "x-windows-950"; break;
        case 25: case 40:   name = "x-windows-949"; break;
        case 26: case 42:   name = "x-Johab";       break;
        case 27:            name = "IBM866";        break;
        case 28:            name = "windows-1250";  break;
        case 29:            name = "windows-1251";  break;
        case 31: case 39:   name = "GBK";           break;
        case 32:            name = "windows-1253";  break;
        case 33:            name = "windows-1254";  break;
        case 34:            name = "windows-1255";  break;
        case 35:            name = "windows-1256";  break;
        case 36:            name = "windows-1257";  break;
        case 37:            name = "x-windows-874"; break;
        case 44:            name = "windows-1258";  break;
        default:            name = "windows-1252";  break;
        }
        try {
            return Charset.forName(name);
        } catch (RuntimeException e) {
            return Charset.forName("windows-1252");
        }
    }
}
