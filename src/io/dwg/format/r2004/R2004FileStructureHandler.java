package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.PageInfo;
import io.dwg.format.common.SectionDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §4 (R2004 DWG FILE FORMAT ORGANIZATION) 구현
 */
public class R2004FileStructureHandler extends AbstractFileStructureHandler {

    // readHeader → readSections 로 전달되는 section map 오프셋
    private long sectionMapOffset;

    @Override
    public DwgVersion version() {
        return DwgVersion.R2004;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2000 || version == DwgVersion.R2004;
    }

    // -------------------------------------------------------------------------
    // readHeader
    // -------------------------------------------------------------------------
    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2004);

        // 파일 헤더 0x80바이트 읽기
        byte[] headerBytes = new byte[0x80];
        for (int i = 0; i < 0x80; i++) {
            headerBytes[i] = (byte) input.readRawChar();
        }

        // XOR 복호화 (처음 6바이트는 암호화되지 않음 - 버전 문자열)
        byte[] decrypted = decryptHeader(headerBytes);

        // 헤더 필드 추출
        // 유지보수 버전: offset 0x0A
        fields.setMaintenanceVersion(decrypted[0x0A] & 0xFF);

        // Preview Offset: offset 0x0C (4바이트)
        fields.setPreviewOffset(readLE32(decrypted, 0x0C));

        // CodePage: offset 0x18 (2바이트)
        fields.setCodePage(readLE16(decrypted, 0x18) & 0xFFFF);

        // Security Flags: offset 0x1C (4바이트)
        fields.setSecurityFlags(readLE32(decrypted, 0x1C));

        // Summary Info Offset: offset 0x24 (4바이트)
        fields.setSummaryInfoOffset(readLE32(decrypted, 0x24));

        // VBA Project Offset: offset 0x28 (8바이트)
        fields.setVbaProjectOffset(readLE64(decrypted, 0x28));

        // Section Map Offset: offset 0x4C (4바이트)
        // 스펙 §4.2: offset 0x4C = sectionMapOffset(RL)
        sectionMapOffset = readLE32(decrypted, 0x4C) & 0xFFFFFFFFL;

        return fields;
    }

    // -------------------------------------------------------------------------
    // readSections
    // -------------------------------------------------------------------------
    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)
            throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        if (sectionMapOffset == 0) return sections;

        // Section Map 읽기
        R2004SectionMap sectionMap = R2004SectionMap.read(input, sectionMapOffset);

        Lz77Decompressor lz77 = new Lz77Decompressor();

        for (SectionDescriptor desc : sectionMap.descriptors()) {
            try {
                byte[] data = assembleSectionData(input, desc, lz77);
                sections.put(desc.name(), new SectionInputStream(data, desc.name()));
            } catch (Exception e) {
                // 개별 섹션 실패는 무시
            }
        }

        return sections;
    }

    /**
     * 여러 페이지의 데이터를 순서대로 읽어 합치고 압축 해제.
     */
    private byte[] assembleSectionData(BitInput input, SectionDescriptor desc,
            Lz77Decompressor lz77) throws Exception {
        // 전체 압축 데이터 누적
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        for (PageInfo page : desc.pages()) {
            input.seek(page.pageOffset() * 8);
            byte[] pageData = new byte[(int) page.dataSize()];
            for (int i = 0; i < pageData.length; i++) {
                pageData[i] = (byte) input.readRawChar();
            }
            baos.write(pageData);
        }

        byte[] compressed = baos.toByteArray();

        if (desc.compressionType() == 2 && desc.uncompressedSize() > 0) {
            return lz77.decompress(compressed, (int) desc.uncompressedSize());
        }
        return compressed;
    }

    // -------------------------------------------------------------------------
    // writeHeader / writeSections (Phase 3)
    // -------------------------------------------------------------------------
    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // TODO: Phase 3 – R2004 헤더 쓰기
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections,
            FileHeaderFields header) throws Exception {
        // TODO: Phase 3 – R2004 섹션 쓰기
    }

    // -------------------------------------------------------------------------
    // XOR 복호화 (§4.1: 처음 6바이트는 암호화 안 됨, 6~0x7F만 XOR)
    // -------------------------------------------------------------------------
    private byte[] decryptHeader(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];

        // 처음 6바이트는 그대로 복사 (버전 문자열은 암호화되지 않음 - 스펙 §4.1)
        for (int i = 0; i < 6; i++) {
            decrypted[i] = encrypted[i];
        }

        // 6바이트부터 0x80까지 XOR 복호화
        int[] magic = generateMagicNumber();
        for (int i = 6; i < 0x80; i++) {
            int magicIndex = i - 6;  // magic은 offset 0부터 시작 (6번째 바이트부터)
            int magicValue = (magicIndex < magic.length) ? magic[magicIndex] : 0;
            decrypted[i] = (byte) (encrypted[i] ^ magicValue);
        }

        // 0x80 이상은 그대로 복사
        for (int i = 0x80; i < encrypted.length; i++) {
            decrypted[i] = encrypted[i];
        }

        return decrypted;
    }

    private int[] generateMagicNumber() {
        // Magic number는 0x7A개 생성 (6부터 0x80까지 = 0x7A개)
        int[] magic = new int[0x7A];
        int seed = 1;
        for (int i = 0; i < magic.length; i++) {
            seed = seed * 0x0343FD + 0x269EC3;
            magic[i] = (seed >> 16) & 0xFF;
        }
        return magic;
    }

    // -------------------------------------------------------------------------
    // Little-endian helpers
    // -------------------------------------------------------------------------
    private static int readLE32(byte[] data, int off) {
        return (data[off] & 0xFF) | ((data[off+1] & 0xFF) << 8)
             | ((data[off+2] & 0xFF) << 16) | ((data[off+3] & 0xFF) << 24);
    }

    private static int readLE16(byte[] data, int off) {
        return (data[off] & 0xFF) | ((data[off+1] & 0xFF) << 8);
    }

    private static long readLE64(byte[] data, int off) {
        return (readLE32(data, off) & 0xFFFFFFFFL)
             | (((long) readLE32(data, off + 4)) << 32);
    }
}
