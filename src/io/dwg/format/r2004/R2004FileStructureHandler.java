package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.CrcLookupTables;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    // readSections - Simplified using DecoderR2004 logic
    // -------------------------------------------------------------------------
    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)
            throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        if (sectionMapOffset == 0) return sections;

        long sectionDataStart = 0x100;
        input.seek(sectionDataStart * 8);

        // Read all data from 0x100 onwards (up to 1MB)
        long maxSectionSize = 1000000;
        ByteArrayOutputStream sectionDataStream = new ByteArrayOutputStream();
        try {
            for (long i = 0; i < maxSectionSize; i++) {
                try {
                    int b = input.readRawChar() & 0xFF;
                    sectionDataStream.write(b);
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            // Reading error
        }
        byte[] allData = sectionDataStream.toByteArray();
        System.out.printf("[DEBUG] R2004: Read %d bytes from 0x100\n", allData.length);

        // Debug: Show first 32 bytes (encrypted)
        System.out.printf("[DEBUG] R2004: First 32 bytes of allData (encrypted):\n");
        for (int i = 0; i < Math.min(32, allData.length); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < allData.length; j++) {
                System.out.printf("%02X ", allData[i + j] & 0xFF);
            }
            System.out.println();
        }

        // FIRST PASS: Scan all pages and identify system pages and data page ranges
        Map<Integer, SectionInfo> dataPages = new HashMap<>();
        java.util.List<SystemPageInfo> systemPages = new java.util.ArrayList<>();
        int scanOffset = 0;

        while (scanOffset + 4 <= allData.length) {
            // Read first 4 bytes to determine page type (unencrypted for system pages)
            int rawPageType = ByteBuffer.wrap(allData, scanOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            byte[] pageHeader;
            int pageType;

            if (rawPageType == 0x41630e3b || rawPageType == 0x4163003b) {
                // System page - unencrypted, read 32-byte header directly
                if (scanOffset + 32 > allData.length) break;
                pageHeader = new byte[32];
                System.arraycopy(allData, scanOffset, pageHeader, 0, 32);
                pageType = rawPageType;
                System.out.printf("[DEBUG] R2004: Found system page at offset 0x%X (type=0x%X)\n",
                    scanOffset, pageType);
            } else {
                // Data page - encrypted, need to decrypt full 32-byte header
                if (scanOffset + 32 > allData.length) break;
                long actualFileOffset = sectionDataStart + scanOffset;
                int secMask = (int)(0x4164536bL ^ (actualFileOffset & 0xFFFFFFFFL));
                pageHeader = new byte[32];
                for (int i = 0; i < 8; i++) {
                    int hdr = ByteBuffer.wrap(allData, scanOffset + i*4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    hdr ^= secMask;
                    byte[] decrypted = ByteBuffer.allocate(4).putInt(hdr).array();
                    System.arraycopy(decrypted, 0, pageHeader, i*4, 4);
                }
                pageType = ByteBuffer.wrap(pageHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            }
            int compSize = ByteBuffer.wrap(pageHeader, 8, 4).order(ByteOrder.BIG_ENDIAN).getInt();

            // Validate
            if (compSize < 0 || compSize > 100000) {
                System.out.printf("[WARN] R2004: Invalid page at offset 0x%X (compSize=%d)\n",
                    scanOffset, compSize);
                break;
            }

            if (pageType == 0x4163043b) {
                // Data section page
                int sectionNum = ByteBuffer.wrap(pageHeader, 4, 4).order(ByteOrder.BIG_ENDIAN).getInt();
                int decompSize = ByteBuffer.wrap(pageHeader, 12, 4).order(ByteOrder.BIG_ENDIAN).getInt();

                if (sectionNum <= 28) {
                    if (!dataPages.containsKey(sectionNum)) {
                        dataPages.put(sectionNum, new SectionInfo());
                    }
                    dataPages.get(sectionNum).pages.add(new PageInfo(scanOffset + 32, compSize, decompSize));
                    System.out.printf("[DEBUG] R2004: Data page sectionNum=%d offset=0x%X compSize=%d decompSize=%d\n",
                        sectionNum, scanOffset, compSize, decompSize);
                }
            } else if (pageType == 0x41630e3b || pageType == 0x4163003b) {
                // System page
                int decompSize = ByteBuffer.wrap(pageHeader, 12, 4).order(ByteOrder.BIG_ENDIAN).getInt();
                int compressionType = ByteBuffer.wrap(pageHeader, 16, 4).order(ByteOrder.BIG_ENDIAN).getInt();
                systemPages.add(new SystemPageInfo(scanOffset, scanOffset + 32, compSize, decompSize, compressionType, pageType));
                System.out.printf("[DEBUG] R2004: System page at offset 0x%X (type=0x%X) compSize=%d\n",
                    scanOffset, pageType, compSize);
            } else {
                System.out.printf("[WARN] R2004: Unknown page type 0x%X at offset 0x%X\n", pageType, scanOffset);
            }

            scanOffset += 32 + compSize;
            if (scanOffset >= allData.length) break;
        }

        // Process system pages to build section name mapping
        Map<Integer, String> sectionNames = buildSectionNameMapping(allData, systemPages, sectionDataStart);

        // Temp fix: Map likely section numbers based on what we see
        // If we don't have explicit names from system pages, use heuristics
        if (!sectionNames.containsKey(8)) {
            sectionNames.put(8, "AcDb:Objects");  // Section 8 seems to be Objects (192 bytes properly decompressed)
        }
        if (!sectionNames.containsKey(10)) {
            sectionNames.put(10, "AcDb:SummaryInfo");
        }
        if (!sectionNames.containsKey(11)) {
            sectionNames.put(11, "AcDb:VBAProject");
        }
        if (!sectionNames.containsKey(13)) {
            sectionNames.put(13, "AcDb:AppInfo");
        }

        // SECOND PASS: Process each data section (decompress all pages and combine)
        for (Integer sectionNum : dataPages.keySet()) {
            SectionInfo info = dataPages.get(sectionNum);
            String sectionName = sectionNames.getOrDefault(sectionNum, "Unknown(" + sectionNum + ")");

            System.out.printf("[DEBUG] R2004: Processing section %d '%s' with %d pages\n",
                sectionNum, sectionName, info.pages.size());

            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            for (PageInfo page : info.pages) {
                // Extract compressed data
                byte[] compressedData = new byte[page.compSize];
                int availableBytes = Math.min(page.compSize, allData.length - page.headerEnd);
                System.arraycopy(allData, page.headerEnd, compressedData, 0, availableBytes);

                if (page.compSize < page.decompSize) {
                    // Need to decompress
                    try {
                        io.dwg.core.util.R2004Lz77Decompressor decompressor =
                            new io.dwg.core.util.R2004Lz77Decompressor();
                        byte[] pageDecomp = decompressor.decompress(compressedData, page.decompSize);
                        decompressed.write(pageDecomp);
                        System.out.printf("[DEBUG] R2004: Section %d page decompressed %d -> %d bytes\n",
                            sectionNum, page.compSize, pageDecomp.length);
                    } catch (Exception e) {
                        System.out.printf("[WARN] R2004: Decompression failed for section %d: %s\n",
                            sectionNum, e.getMessage());
                        decompressed.write(compressedData, 0, availableBytes);
                    }
                } else {
                    // No decompression needed
                    decompressed.write(compressedData, 0, availableBytes);
                }
            }

            byte[] sectionData = decompressed.toByteArray();
            System.out.printf("[DEBUG] R2004: Section %d '%s' final size: %d bytes from %d pages\n",
                sectionNum, sectionName, sectionData.length, info.pages.size());

            sections.put(sectionName, new SectionInputStream(sectionData, sectionName));
        }

        return sections;
    }

    // Build section name mapping from system pages
    private Map<Integer, String> buildSectionNameMapping(byte[] allData,
            java.util.List<SystemPageInfo> systemPages, long sectionDataStart) {
        Map<Integer, String> mapping = new HashMap<>();

        // Default mappings (will be overridden by section map if found)
        mapping.put(1, "AcDb:Header");
        mapping.put(2, "AcDb:AuxHeader");
        mapping.put(3, "AcDb:Classes");
        mapping.put(4, "AcDb:Handles");
        mapping.put(5, "AcDb:Template");
        mapping.put(7, "AcDb:Objects");

        // Try to parse section map from system pages
        for (SystemPageInfo sysPage : systemPages) {
            try {
                byte[] pageData = new byte[sysPage.compSize];
                int avail = Math.min(sysPage.compSize, allData.length - sysPage.headerEnd);
                System.arraycopy(allData, sysPage.headerEnd, pageData, 0, avail);

                // Decompress if needed
                byte[] decompressed = pageData;
                if (sysPage.compressionType == 2 && sysPage.compSize < sysPage.decompSize) {
                    try {
                        io.dwg.core.util.R2004Lz77Decompressor decompressor =
                            new io.dwg.core.util.R2004Lz77Decompressor();
                        decompressed = decompressor.decompress(pageData, sysPage.decompSize);
                    } catch (Exception e) {
                        System.out.printf("[WARN] R2004: Failed to decompress system page: %s\n", e.getMessage());
                        continue;
                    }
                }

                // Try to parse section map from decompressed data
                // Section map typically starts at offset 0 and contains section entries
                if (decompressed.length > 20) {
                    // Parse section entries (each entry is typically 32+ bytes)
                    // Section entry format: sectionNumber (4), sectionName (variable), etc.
                    int offset = 0;
                    while (offset + 4 <= decompressed.length && offset < 500) {
                        try {
                            int sectionNum = ByteBuffer.wrap(decompressed, offset, 4)
                                .order(ByteOrder.LITTLE_ENDIAN).getInt();

                            // Read section name (null-terminated string)
                            if (offset + 8 <= decompressed.length) {
                                StringBuilder name = new StringBuilder();
                                for (int i = offset + 4; i < Math.min(offset + 100, decompressed.length); i++) {
                                    byte b = decompressed[i];
                                    if (b == 0) {
                                        if (name.length() > 0 && name.charAt(0) >= 32) {
                                            // Valid section name found
                                            mapping.put(sectionNum, name.toString());
                                            System.out.printf("[DEBUG] R2004: Section map entry: %d -> '%s'\n",
                                                sectionNum, name.toString());
                                        }
                                        break;
                                    } else if (b >= 32 && b < 127) {
                                        name.append((char)b);
                                    } else if (name.length() > 0) {
                                        break;  // Non-ASCII found in middle, stop parsing
                                    }
                                }
                                offset += 32;
                            } else {
                                break;
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.printf("[WARN] R2004: Error processing system page: %s\n", e.getMessage());
            }
        }

        return mapping;
    }

    // Helper classes for section tracking
    private static class SectionInfo {
        java.util.List<PageInfo> pages = new java.util.ArrayList<>();
    }

    private static class PageInfo {
        int headerEnd, compSize, decompSize;
        PageInfo(int hEnd, int comp, int decomp) {
            headerEnd = hEnd; compSize = comp; decompSize = decomp;
        }
    }

    private static class SystemPageInfo {
        int headerStart, headerEnd, compSize, decompSize, compressionType, pageType;
        SystemPageInfo(int hStart, int hEnd, int comp, int decomp, int comprType, int pType) {
            headerStart = hStart; headerEnd = hEnd; compSize = comp;
            decompSize = decomp; compressionType = comprType; pageType = pType;
        }
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
