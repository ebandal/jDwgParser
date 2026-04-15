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
        return version == DwgVersion.R2004;
    }

    // -------------------------------------------------------------------------
    // readHeader
    // -------------------------------------------------------------------------
    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2004);

        // 파일 헤더 구조 (스펙 §4.1, DecoderR2004 기반):
        // 0x00-0x05: "AC1018" (버전 문자열)
        // 0x06-0x79: 라이브 데이터 필드들 (미암호화)
        // 0x7A-0xE5: 암호화된 헤더 (0x6C 바이트)
        // 0xE6-0xFF: 패딩

        // 1. 버전 문자열 (0x00-0x05) 검증
        byte[] versionBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            versionBytes[i] = (byte) input.readRawChar();
        }
        String version = new String(versionBytes, StandardCharsets.US_ASCII);
        if (!version.equals("AC1018")) {
            throw new IllegalArgumentException("Invalid R2004 version string: " + version);
        }

        // 2. 라이브 데이터 필드 파싱 (0x06-0x7F, 0x7A바이트)
        byte[] liveDataFields = new byte[0x7A]; // 0x7F - 0x06 + 1 = 0x7A
        for (int i = 0; i < 0x7A; i++) {
            liveDataFields[i] = (byte) input.readRawChar();
        }

        // 라이브 데이터 필드 파싱 (DecoderR2004 참조)
        int liveOffset = 0;

        // 0x06-0x0A: 5 bytes (unknown)
        liveOffset += 5;

        // 0x0B: Maintenance release version
        int maintenanceVersion = liveDataFields[liveOffset++] & 0xFF;
        fields.setMaintenanceVersion(maintenanceVersion);

        // 0x0C: Unknown
        liveOffset += 1;

        // 0x0D-0x10: Preview address (4 bytes, LE32)
        int previewOffset = readLE32(liveDataFields, liveOffset);
        fields.setPreviewOffset(previewOffset & 0xFFFFFFFFL);
        liveOffset += 4;

        // 0x11: Application version
        liveOffset += 1;

        // 0x12: Application maintenance version
        liveOffset += 1;

        // 0x13-0x14: Code page (2 bytes, LE16)
        int codePage = readLE16(liveDataFields, liveOffset) & 0xFFFF;
        fields.setCodePage(codePage);
        liveOffset += 2;

        // 0x15-0x17: 3 bytes (unknown)
        liveOffset += 3;

        // 0x18-0x1B: Security flags (4 bytes, LE32)
        int securityFlags = readLE32(liveDataFields, liveOffset);
        fields.setSecurityFlags(securityFlags);
        liveOffset += 4;

        // 0x1C-0x1F: Unknown (4 bytes)
        liveOffset += 4;

        // 0x20-0x23: Summary info offset (4 bytes, LE32)
        int summaryInfoOffset = readLE32(liveDataFields, liveOffset);
        fields.setSummaryInfoOffset(summaryInfoOffset & 0xFFFFFFFFL);
        liveOffset += 4;

        // 0x24-0x27: VBA project offset (4 bytes, LE32)
        int vbaOffset = readLE32(liveDataFields, liveOffset);
        fields.setVbaProjectOffset(vbaOffset & 0xFFFFFFFFL);
        liveOffset += 4;

        // 0x28-0x2B: Unknown (4 bytes)
        liveOffset += 4;

        // 0x2C-0x2F: Unknown (4 bytes) - 섹션맵 오프셋은 암호화된 헤더에서 읽음
        liveOffset += 4;

        // 3. 파일의 0x7A-0xE5 (암호화된 헤더) 읽기
        byte[] encryptedHeader = new byte[0x6C];
        for (int i = 0; i < 0x6C; i++) {
            encryptedHeader[i] = (byte) input.readRawChar();
        }

        // 4. 암호화된 헤더 복호화
        byte[] decryptedHeader = decryptR2004Header(encryptedHeader);

        // DEBUG: 복호화된 헤더 처음 0x40 바이트 출력
        System.out.println("[DEBUG] Decrypted header (first 0x40 bytes):");
        for (int i = 0; i < 0x40; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < decryptedHeader.length; j++) {
                System.out.printf("%02X ", decryptedHeader[i + j] & 0xFF);
            }
            System.out.println();
        }
        // DEBUG: file_ID_string 확인
        String fileId = new String(decryptedHeader, 0, 12, StandardCharsets.US_ASCII);
        System.out.printf("[DEBUG] file_ID_string: \"%s\" (should be \"AcFssFcAJMB\")\n", fileId);

        // 5. 복호화된 헤더에서 섹션맵 오프셋 추출 (0x54-0x5B)
        sectionMapOffset = readLE64(decryptedHeader, 0x54) & 0xFFFFFFFFFFFFFFFFL;
        System.out.printf("[DEBUG] Section map address at 0x54-0x5B: raw=0x%016X\n", sectionMapOffset);

        // 6. 복호화된 헤더 CRC 검증
        verifyCrc32(decryptedHeader);

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
     * 복호화된 R2004 헤더의 CRC32를 검증합니다.
     * 스펙 §4.1: r2004_file_header.spec 참고
     */
    private void verifyCrc32(byte[] decryptedHeader) {
        // CRC32 값 추출 (0x68-0x6B)
        int storedCrc32 = readLE32(decryptedHeader, 0x68);

        // CRC 검증을 위해 CRC 필드를 0으로 설정
        byte[] dataForCrc = decryptedHeader.clone();
        dataForCrc[0x68] = 0;
        dataForCrc[0x69] = 0;
        dataForCrc[0x6A] = 0;
        dataForCrc[0x6B] = 0;

        // CRC32 계산
        int calculatedCrc32 = calculateCrc32(dataForCrc, 0, 0x6C);

        // CRC 검증 (경고만, 실패는 아님)
        if (storedCrc32 != calculatedCrc32) {
            System.out.println("WARNING: R2004 header CRC32 mismatch. "
                + String.format("Stored: 0x%08x, Calculated: 0x%08x",
                    storedCrc32, calculatedCrc32));
        }
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
    // writeHeader / writeSections
    // -------------------------------------------------------------------------
    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // R2004 헤더 쓰기
        System.out.println("[DEBUG] R2004.writeHeader: start");

        // 1. Version string (6 bytes) - "AC1018"
        byte[] versionStr = "AC1018".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (byte b : versionStr) {
            output.writeRawChar(b & 0xFF);
        }

        // 2. Live data fields (0x06-0x7A, 0x7A bytes)
        byte[] liveData = new byte[0x7A];
        // 0x06-0x0A: Preview address (5 bytes) - placeholder
        // 0x0B-0x0F: App version (5 bytes) - placeholder
        // 나머지는 0으로 초기화
        for (int i = 0; i < liveData.length; i++) {
            output.writeRawChar(0);
        }

        // 3. 암호화된 헤더 생성 (0x7A-0xE5, 0x6C bytes)
        byte[] headerToEncrypt = buildEncryptedHeaderData(header);

        // 4. 헤더 암호화
        byte[] encryptedHeader = encryptR2004Header(headerToEncrypt);

        // 5. 암호화된 헤더 쓰기
        for (byte b : encryptedHeader) {
            output.writeRawChar(b & 0xFF);
        }

        // 6. 패딩 (0xE6-0xFF, 0x1A bytes)
        for (int i = 0; i < 0x1A; i++) {
            output.writeRawChar(0);
        }

        System.out.println("[DEBUG] R2004.writeHeader: encrypted header written, size=" + encryptedHeader.length);
    }

    /**
     * 암호화할 헤더 데이터 구성 (0x6C = 108 bytes)
     */
    private byte[] buildEncryptedHeaderData(FileHeaderFields header) {
        byte[] data = new byte[0x6C];

        // 0x00-0x0B: File ID string "AcFssFcAJMB"
        String fileId = "AcFssFcAJMB";
        byte[] fileIdBytes = fileId.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(fileIdBytes, 0, data, 0, Math.min(fileIdBytes.length, 12));

        // 0x0C-0x53: Reserved/padding (0x48 bytes)
        // 0x54-0x5B: Section map offset (8 bytes, LE64) - placeholder 0
        // 0x5C-0x67: Unknown (0x0C bytes)
        // 0x68-0x6B: CRC32 (4 bytes, LE32) - will be calculated

        // CRC32 계산 (CRC 필드는 0으로)
        int crc32 = calculateCrc32(data, 0, 0x6C);

        // CRC32 값 설정 (0x68-0x6B, little-endian)
        writeLE32(data, 0x68, crc32);

        return data;
    }

    /**
     * 암호화된 R2004 헤더를 생성합니다.
     * 복호화의 역과정: XOR with LCG stream
     */
    private byte[] encryptR2004Header(byte[] plaintext) {
        byte[] encrypted = new byte[plaintext.length];
        int rseed = 1;

        for (int i = 0; i < plaintext.length; i++) {
            rseed = (int)((rseed * 0x343fdL + 0x269ec3L) & 0xFFFFFFFFL);
            encrypted[i] = (byte) (plaintext[i] ^ ((rseed >> 16) & 0xFF));
        }

        return encrypted;
    }

    /**
     * Little-endian 32비트 쓰기
     */
    private void writeLE32(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections,
            FileHeaderFields header) throws Exception {
        // R2004 섹션 쓰기
        // 섹션 맵 오프셋은 파일 끝에 있음
        // 각 섹션은 순서대로 작성됨

        // 단순 구현: 섹션을 그대로 작성 (LZ77 압축 제외)
        for (String sectionName : sections.keySet()) {
            byte[] sectionData = sections.get(sectionName);
            if (sectionData != null) {
                for (byte b : sectionData) {
                    output.writeRawChar(b & 0xFF);
                }
            }
        }

        // TODO: Phase 3 – 섹션 맵 작성, 암호화 처리, LZ77 압축
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
