package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.CrcLookupTables;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.SectionDescriptor;

import java.io.ByteArrayOutputStream;
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
        int previewOffset = (int) ByteUtils.readLE32(liveDataFields, liveOffset);
        fields.setPreviewOffset(previewOffset & 0xFFFFFFFFL);
        liveOffset += 4;

        // 0x11: Application version
        liveOffset += 1;

        // 0x12: Application maintenance version
        liveOffset += 1;

        // 0x13-0x14: Code page (2 bytes, LE16)
        int codePage = ByteUtils.readLE16(liveDataFields, liveOffset) & 0xFFFF;
        fields.setCodePage(codePage);
        liveOffset += 2;

        // 0x15-0x17: 3 bytes (unknown)
        liveOffset += 3;

        // 0x18-0x1B: Security flags (4 bytes, LE32)
        int securityFlags = (int) ByteUtils.readLE32(liveDataFields, liveOffset);
        fields.setSecurityFlags(securityFlags);
        liveOffset += 4;

        // 0x1C-0x1F: Unknown (4 bytes)
        liveOffset += 4;

        // 0x20-0x23: Summary info offset (4 bytes, LE32)
        int summaryInfoOffset = (int) ByteUtils.readLE32(liveDataFields, liveOffset);
        fields.setSummaryInfoOffset(summaryInfoOffset & 0xFFFFFFFFL);
        liveOffset += 4;

        // 0x24-0x27: VBA project offset (4 bytes, LE32)
        int vbaOffset = (int) ByteUtils.readLE32(liveDataFields, liveOffset);
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

        // DEBUG: 전체 복호화된 헤더 출력
        System.out.println("[DEBUG] Decrypted header (full 0x6C bytes):");
        for (int i = 0; i < Math.min(0x6C, decryptedHeader.length); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < decryptedHeader.length; j++) {
                System.out.printf("%02X ", decryptedHeader[i + j] & 0xFF);
            }
            System.out.println();
        }
        // DEBUG: file_ID_string 확인
        String fileId = new String(decryptedHeader, 0, 12, StandardCharsets.US_ASCII);
        System.out.printf("[DEBUG] file_ID_string: \"%s\" (should be \"AcFssFcAJMB\")\n", fileId);

        // DEBUG: 특정 오프셋의 값들 확인
        System.out.printf("[DEBUG] decryptedHeader[0x24] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x24) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x28] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x28) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x2C] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x2C) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x50] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x50) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x54] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x54) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x58] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x58) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x5C] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x5C) & 0xFFFFFFFFL);
        System.out.printf("[DEBUG] decryptedHeader[0x60] = 0x%08X\n", ByteUtils.readLE32(decryptedHeader, 0x60) & 0xFFFFFFFFL);

        // 5. 복호화된 헤더에서 섹션맵 오프셋 추출 (0x54-0x5B)
        sectionMapOffset = ByteUtils.readLE64(decryptedHeader, 0x54) & 0xFFFFFFFFFFFFFFFFL;
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
        int storedCrc32 = (int) ByteUtils.readLE32(decryptedHeader, 0x68);

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

        // Read R2004 Section Map
        R2004SectionMap sectionMap = R2004SectionMap.read(input, sectionMapOffset);
        System.out.printf("[DEBUG] R2004: Section map loaded with %d sections\n", sectionMap.descriptors().size());

        // Process each section
        for (SectionDescriptor desc : sectionMap.descriptors()) {
            try {
                if (desc.offset() == 0 || desc.uncompressedSize() == 0) continue;

                long sectionSize = desc.uncompressedSize();

                // Limit to reasonable size
                if (sectionSize <= 0 || sectionSize > 10000000) {
                    System.out.printf("[WARN] R2004: Section '%s' has unreasonable size %d, skipping\n",
                        desc.name(), sectionSize);
                    continue;
                }

                // Read raw section data from file
                input.seek(desc.offset() * 8); // offset is in bytes, seek expects bits
                byte[] data = new byte[(int) sectionSize];
                for (int i = 0; i < sectionSize; i++) {
                    try {
                        data[i] = (byte) input.readRawChar();
                    } catch (Exception e) {
                        // Truncate if we can't read more
                        byte[] trimmed = new byte[i];
                        System.arraycopy(data, 0, trimmed, 0, i);
                        data = trimmed;
                        System.out.printf("[WARN] R2004: Section '%s' truncated at %d bytes\n", desc.name(), i);
                        break;
                    }
                }

                System.out.printf("[DEBUG] R2004: Section '%s' - read %d bytes (0x%X to 0x%X)\n",
                    desc.name(), data.length, desc.offset(), desc.offset() + sectionSize);

                // R2004 sections are stored with 32-byte encrypted page headers
                // Format: [32-byte encrypted header] [compressed/uncompressed data] [repeat for multiple pages]
                byte[] sectionData = data;

                // Check if this section spans multiple pages
                System.out.printf("[DEBUG] R2004: Processing section '%s' (%d bytes total)\n", desc.name(), data.length);

                ByteArrayOutputStream combinedDecompressed = new ByteArrayOutputStream();
                int pageOffset = 0;
                int pageCount = 0;

                while (pageOffset < data.length && pageOffset + 32 <= data.length) {
                    pageCount++;
                    System.out.printf("[DEBUG] R2004: Reading page %d at offset %d\n", pageCount, pageOffset);
                    // Decrypt page header - secMask based on ACTUAL file offset
                    long actualFileOffset = desc.offset() + pageOffset;
                    long secMask = 0x4164536bL ^ (actualFileOffset & 0xFFFFFFFFL);
                    System.out.printf("[DEBUG] R2004: File offset=0x%X, secMask=0x%X\n", actualFileOffset, secMask);
                    byte[] pageHeader = new byte[32];
                    for (int i = 0; i < 32; i++) {
                        pageHeader[i] = (byte)(data[pageOffset + i] ^ ((secMask >> ((i & 3) * 8)) & 0xFF));
                    }

                    // Parse page header
                    int pageType = (int)(ByteUtils.readLE32(pageHeader, 0) & 0xFFFFFFFFL);
                    int sectionNum = (int)(ByteUtils.readLE32(pageHeader, 4) & 0xFFFFFFFFL);
                    int compSize = (int)(ByteUtils.readLE32(pageHeader, 8) & 0xFFFFFFFFL);
                    int decompSize = (int)(ByteUtils.readLE32(pageHeader, 12) & 0xFFFFFFFFL);
                    int startOffset = (int)(ByteUtils.readLE32(pageHeader, 16) & 0xFFFFFFFFL);

                    // Debug: Show decrypted page header bytes for Header
                    if ("AcDb:Header".equals(desc.name())) {
                        System.out.printf("[DEBUG] R2004: Header decrypted page header bytes:\n");
                        for (int i = 0; i < 32; i += 8) {
                            System.out.printf("  0x%02X: ", i);
                            for (int j = 0; j < 8; j++) {
                                System.out.printf("%02X ", pageHeader[i + j] & 0xFF);
                            }
                            System.out.println();
                        }
                    }

                    System.out.printf("[DEBUG] R2004: '%s' page header - type=0x%X comp=%d decomp=%d\n",
                        desc.name(), pageType, compSize, decompSize);

                    if (pageType == 0x4163043B) { // Data section page
                        // Extract compressed data (skip 32-byte header)
                        byte[] compressedData = new byte[compSize];
                        int availableBytes = Math.min(compSize, data.length - pageOffset - 32);
                        System.arraycopy(data, pageOffset + 32, compressedData, 0, availableBytes);

                        // Debug first bytes of compressed data for AuxHeader
                        if ("AcDb:AuxHeader".equals(desc.name()) && availableBytes > 0) {
                            System.out.printf("[DEBUG] R2004: AuxHeader compressed data first 64 bytes (hex):\n");
                            for (int i = 0; i < Math.min(64, availableBytes); i += 16) {
                                System.out.printf("  0x%02X: ", i);
                                for (int j = 0; j < 16 && i + j < availableBytes; j++) {
                                    System.out.printf("%02X ", compressedData[i + j] & 0xFF);
                                }
                                System.out.println();
                            }
                        }

                        // Try to decompress if needed
                        if (compSize < decompSize) {
                            try {
                                System.out.printf("[DEBUG] R2004: '%s' page at offset %d: decompressing %d->%d bytes\n",
                                    desc.name(), pageOffset, compSize, decompSize);
                                io.dwg.core.util.R2004Lz77Decompressor decompressor = new io.dwg.core.util.R2004Lz77Decompressor();
                                byte[] pageDecompressed = decompressor.decompress(compressedData, decompSize);
                                combinedDecompressed.write(pageDecompressed);
                                System.out.printf("[DEBUG] R2004: Page decompressed to %d bytes\n", pageDecompressed.length);
                            } catch (Exception e) {
                                System.out.printf("[WARN] R2004: Failed to decompress page: %s\n", e.toString());
                                combinedDecompressed.write(compressedData, 0, availableBytes);
                            }
                        } else {
                            combinedDecompressed.write(compressedData, 0, availableBytes);
                        }
                    } else {
                        // Not a standard page, skip it
                        System.out.printf("[DEBUG] R2004: Non-data page type 0x%X, skipping\n", pageType);
                    }

                    // Move to next page (32-byte header + compSize bytes of data)
                    pageOffset += 32 + compSize;
                }

                // Use combined decompressed data from all pages
                if (combinedDecompressed.size() > 0) {
                    sectionData = combinedDecompressed.toByteArray();
                    System.out.printf("[DEBUG] R2004: Section '%s' total decompressed: %d bytes from %d input bytes\n",
                        desc.name(), sectionData.length, data.length);
                } else {
                    sectionData = data;  // Fallback to raw data if no pages decompressed
                }

                // Debug output for Classes section
                if ("AcDb:Classes".equals(desc.name()) && sectionData.length > 0) {
                    System.out.printf("[DEBUG] R2004: Classes section processed size: %d bytes\n", sectionData.length);
                    System.out.printf("[DEBUG] R2004: Classes section first 64 bytes (processed):\n");
                    for (int i = 0; i < Math.min(64, sectionData.length); i += 16) {
                        System.out.printf("  0x%02X: ", i);
                        for (int j = 0; j < 16 && i + j < sectionData.length; j++) {
                            System.out.printf("%02X ", sectionData[i + j] & 0xFF);
                        }
                        System.out.println();
                    }
                    // Check for sentinel
                    if (sectionData.length >= 16) {
                        byte[] sentinel = {(byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9, (byte)0xF8, (byte)0xC5,
                                         (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F, (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A};
                        boolean found = true;
                        for (int i = 0; i < 16; i++) {
                            if (sectionData[i] != sentinel[i]) {
                                found = false;
                                break;
                            }
                        }
                        System.out.printf("[DEBUG] R2004: Classes sentinel found at offset 0: %b\n", found);
                    }
                }

                sections.put(desc.name(), new SectionInputStream(sectionData, desc.name()));

            } catch (Exception e) {
                System.out.printf("[WARN] R2004FileStructureHandler: Failed to read section '%s': %s\n",
                    desc.name(), e.getMessage());
                e.printStackTrace();
            }
        }

        return sections;
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

        // 0x54-0x5B: Section map offset (8 bytes, LE64)
        long sectionMapOffset = header.sectionMapOffset();
        writeLE64(data, 0x54, sectionMapOffset);
        System.out.println("[DEBUG] Header: section map offset set to 0x" + Long.toHexString(sectionMapOffset));

        // 0x5C-0x67: Unknown (0x0C bytes)
        // 0x68-0x6B: CRC32 (4 bytes, LE32) - will be calculated

        // CRC32 계산 (CRC 필드는 0으로)
        int crc32 = calculateCrc32(data, 0, 0x6C);

        // CRC32 값 설정 (0x68-0x6B, little-endian)
        writeLE32(data, 0x68, crc32);

        return data;
    }

    /**
     * Little-endian 64비트 쓰기
     */
    private void writeLE64(byte[] data, int offset, long value) {
        writeLE32(data, offset, (int)(value & 0xFFFFFFFF));
        writeLE32(data, offset + 4, (int)((value >>> 32) & 0xFFFFFFFF));
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
        // 구조: [섹션 데이터] + [섹션 맵]

        // 1. 섹션 데이터 쓰기
        long currentOffset = 0x100 / 0x100;  // Header는 0x100 바이트 (0 pages로 계산)
        java.util.Map<String, Long> sectionOffsets = new java.util.HashMap<>();
        java.util.Map<String, Long> sectionSizes = new java.util.HashMap<>();

        System.out.println("[DEBUG] writeSections: Writing section data...");
        for (String sectionName : sections.keySet()) {
            byte[] sectionData = sections.get(sectionName);
            if (sectionData != null && sectionData.length > 0) {
                sectionOffsets.put(sectionName, currentOffset);
                sectionSizes.put(sectionName, (long)sectionData.length);

                // 섹션 데이터 쓰기
                for (byte b : sectionData) {
                    output.writeRawChar(b & 0xFF);
                }

                currentOffset += sectionData.length;
                System.out.println("  [DEBUG] Section '" + sectionName + "': offset=0x" +
                    Long.toHexString(sectionOffsets.get(sectionName)) + ", size=0x" +
                    Long.toHexString((long)sectionData.length));
            }
        }

        // 2. 섹션 맵 쓰기
        long sectionMapStartOffset = currentOffset;
        System.out.println("[DEBUG] Section map starts at offset 0x" + Long.toHexString(sectionMapStartOffset));

        // Section count (RL - 4 bytes)
        int sectionCount = 0;
        for (String sectionName : sections.keySet()) {
            if (sections.get(sectionName) != null && sections.get(sectionName).length > 0) {
                sectionCount++;
            }
        }
        output.writeRawLong(sectionCount);
        System.out.println("[DEBUG] Section count: " + sectionCount);

        // 각 섹션 descriptor 쓰기
        for (String sectionName : sections.keySet()) {
            byte[] sectionData = sections.get(sectionName);
            if (sectionData != null && sectionData.length > 0) {
                writeR2004DataSectionDescriptor(output, sectionName, sectionData,
                    sectionOffsets.get(sectionName));
            }
        }

        // 3. Section map CRC32 (TODO: Phase 3+)
        // 현재는 생략

        System.out.println("[DEBUG] writeSections: Complete. Final offset=0x" + Long.toHexString(currentOffset));
    }

    /**
     * R2004 Data Section Descriptor 쓰기
     * Format (per R2004DataSectionDescriptor.read):
     * - compressedSize (RL, 4 bytes)
     * - uncompressedSize (RL, 4 bytes)
     * - compressionType (RL, 4 bytes) - 0=none, 2=LZ77
     * - 3 x unknown (RL, 12 bytes)
     * - sectionName (64 bytes, UTF-16LE)
     * - pageCount (RL, 4 bytes)
     * - pages: for each page:
     *   - pageId (RL, 4 bytes)
     *   - dataSize (RL, 4 bytes)
     *   - pageOffset (RL, 4 bytes)
     */
    private void writeR2004DataSectionDescriptor(BitOutput output, String sectionName,
            byte[] sectionData, long sectionStartOffset) throws Exception {

        int compressedSize = sectionData.length;
        int uncompressedSize = sectionData.length;
        int compressionType = 0;  // No compression

        // compressedSize (RL)
        output.writeRawLong(compressedSize);
        // uncompressedSize (RL)
        output.writeRawLong(uncompressedSize);
        // compressionType (RL)
        output.writeRawLong(compressionType);

        // 3 x unknown (RL) - reserved fields
        output.writeRawLong(0);
        output.writeRawLong(0);
        output.writeRawLong(0);

        // sectionName (64 bytes, UTF-16LE, null-terminated)
        byte[] nameBytes = sectionName.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        for (int i = 0; i < 64; i++) {
            if (i < nameBytes.length) {
                output.writeRawChar(nameBytes[i] & 0xFF);
            } else {
                output.writeRawChar(0);
            }
        }

        // pageCount (RL) - single page for simplicity
        int pageCount = 1;
        output.writeRawLong(pageCount);

        // Page descriptor
        // pageId (RL)
        output.writeRawLong(0);
        // dataSize (RL)
        output.writeRawLong(sectionData.length);
        // pageOffset (RL) - in page units (0x100 bytes)
        output.writeRawLong((int)(sectionStartOffset & 0xFFFFFFFFL));

        System.out.println("  [DEBUG] Descriptor for '" + sectionName + "': size=0x" +
            Integer.toHexString(sectionData.length));
    }


}
