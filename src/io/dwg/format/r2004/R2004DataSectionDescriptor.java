package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.format.common.PageInfo;
import io.dwg.format.common.SectionDescriptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * §4 Data Section Map 엔트리: 섹션명 + 압축 정보 + 페이지 목록.
 */
public class R2004DataSectionDescriptor {

    public static SectionDescriptor read(BitInput input) {
        long compressedSize   = input.readRawLong() & 0xFFFFFFFFL;
        long uncompressedSize = input.readRawLong() & 0xFFFFFFFFL;
        int  compressionType  = input.readRawLong();  // 0=none, 2=LZ77
        // unknown, encrypt flags, unknown2 – 예약 필드 skip
        input.readRawLong();
        input.readRawLong();
        input.readRawLong();

        // 섹션 이름 (64바이트 고정 길이 UTF-16LE)
        byte[] nameBytes = new byte[64];
        for (int i = 0; i < 64; i++) nameBytes[i] = (byte) input.readRawChar();
        String name = parseUtf16Name(nameBytes);

        int pageCount = input.readRawLong();

        List<PageInfo> pages = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            R2004PageDescriptor pd = R2004PageDescriptor.read(input);
            pages.add(new PageInfo(pd.pageOffset(), pd.dataSize(), pd.pageId()));
        }

        SectionDescriptor desc = new SectionDescriptor(name);
        desc.setCompressedSize(compressedSize);
        desc.setUncompressedSize(uncompressedSize);
        desc.setCompressionType(compressionType);
        pages.forEach(desc::addPage);
        return desc;
    }

    private static String parseUtf16Name(byte[] bytes) {
        // null-terminated UTF-16LE
        int len = 0;
        while (len + 1 < bytes.length && (bytes[len] != 0 || bytes[len + 1] != 0)) {
            len += 2;
        }
        return new String(bytes, 0, len, StandardCharsets.UTF_16LE);
    }
}
