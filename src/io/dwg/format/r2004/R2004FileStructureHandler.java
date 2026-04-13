package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.CrcLookupTables;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.PageInfo;
import io.dwg.format.common.SectionDescriptor;

import java.nio.charset.StandardCharsets;
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

        // 파일 헤더 구조 (스펙 §4.1):
        // 0x00-0x05: "AC1018" (버전 문자열, 미암호화)
        // 0x06-0x7F: 라이브 데이터 (0x7A 바이트, 미암호화)
        // 0x80-0xEB: 암호화된 헤더 (0x6C 바이트)
        // 0xEC-0xFF: 패딩 (0x14 바이트)

        // 1. 파일의 0x00-0x7F (라이브 데이터) 읽기
        byte[] liveData = new byte[0x80];
        for (int i = 0; i < 0x80; i++) {
            liveData[i] = (byte) input.readRawChar();
        }

        // 2. 파일의 0x80-0xEB (암호화된 헤더) 읽기
        byte[] encryptedHeader = new byte[0x6C];
        for (int i = 0; i < 0x6C; i++) {
            encryptedHeader[i] = (byte) input.readRawChar();
        }

        // 3. 암호화된 헤더 복호화
        byte[] decryptedHeader = decryptR2004Header(encryptedHeader);

        // 4. 복호화된 헤더 파싱
        parseDecryptedHeader(decryptedHeader, fields);

        // 5. 라이브 데이터에서 섹션맵 오프셋 추출 (0x2C-0x2F)
        sectionMapOffset = readLE32(liveData, 0x2C) & 0xFFFFFFFFL;

        return fields;
    }

    /**
     * 암호화된 R2004 헤더를 복호화합니다.
     * libredwg 구현을 기반으로 함: decrypt_R2004_header()
     */
    private byte[] decryptR2004Header(byte[] encryptedData) {
        byte[] decrypted = new byte[encryptedData.length];
        int rseed = 1;

        for (int i = 0; i < encryptedData.length; i++) {
            rseed = (int)((rseed * 0x343fdL + 0x269ec3L) & 0xFFFFFFFFL);
            decrypted[i] = (byte) (encryptedData[i] ^ ((rseed >> 16) & 0xFF));
        }

        return decrypted;
    }

    /**
     * 복호화된 R2004 헤더를 파싱합니다.
     * 스펙 §4.1: r2004_file_header.spec 참고
     */
    private void parseDecryptedHeader(byte[] data, FileHeaderFields fields) {
        // 0x00-0x0B: file_ID_string ("AcFssFcAJMB" - 12바이트)
        String fileId = new String(data, 0, 12, StandardCharsets.US_ASCII);

        // 0x0C-0x0F: header_address (4바이트, RLx)
        // 0x10-0x13: header_size (4바이트, RL)
        // 0x14-0x17: x04 (4바이트, RL)

        // 0x18-0x1B: root_tree_node_gap (4바이트)
        // 0x1C-0x1F: lowermost_left_tree_node_gap (4바이트)
        // 0x20-0x23: lowermost_right_tree_node_gap (4바이트)

        // 0x24-0x27: unknown_long (4바이트)
        // 0x28-0x2B: last_section_id (4바이트)
        // 0x2C-0x33: last_section_address (8바이트)
        // 0x34-0x3B: secondheader_address (8바이트)
        // 0x3C-0x3F: numgaps (4바이트)
        // 0x40-0x43: numsections (4바이트)
        // 0x44-0x47: x20 (4바이트)
        // 0x48-0x4B: x80 (4바이트)
        // 0x4C-0x4F: x40 (4바이트)
        // 0x50-0x53: section_map_id (4바이트)
        // 0x54-0x5B: section_map_address (8바이트)
        // 0x5C-0x5F: section_info_id (4바이트)
        // 0x60-0x63: section_array_size (4바이트)
        // 0x64-0x67: gap_array_size (4바이트)
        // 0x68-0x6B: crc32 (4바이트, RLx)

        // CRC32 값 추출 (0x68-0x6B)
        int storedCrc32 = readLE32(data, 0x68);

        // CRC 검증을 위해 CRC 필드를 0으로 설정
        byte[] dataForCrc = data.clone();
        dataForCrc[0x68] = 0;
        dataForCrc[0x69] = 0;
        dataForCrc[0x6A] = 0;
        dataForCrc[0x6B] = 0;

        // CRC32 계산
        int calculatedCrc32 = calculateCrc32(dataForCrc, 0, 0x6C);

        // CRC 검증 (경고만, 실패는 아님)
        if (storedCrc32 != calculatedCrc32) {
            System.out.println("WARNING: R2004 header CRC32 mismatch. "
                + String.format("Expected: 0x%08x, Calculated: 0x%08x",
                    storedCrc32, calculatedCrc32));
        }

        // 주요 필드 설정
        fields.setMaintenanceVersion(0); // 라이브 데이터에서 추출 가능
    }

    /**
     * CRC-32 계산 (R2004+ 헤더용)
     * libredwg bit_calc_CRC32() 기반
     */
    private int calculateCrc32(byte[] data, int offset, int length) {
        // 초기값: ~seed (seed=0이므로 0xFFFFFFFF)
        int crc = 0xFFFFFFFF;

        for (int i = offset; i < offset + length; i++) {
            int byte_val = data[i] & 0xFF;
            // crc = (crc >> 8) ^ table[(crc ^ byte) & 0xff]
            crc = CrcLookupTables.CRC32_TABLE[(crc ^ byte_val) & 0xFF] ^ (crc >>> 8);
        }

        // 최종값을 반전
        return crc ^ 0xFFFFFFFF;
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
