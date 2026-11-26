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

    // readHeader → readSections 로 전달되는 값들
    private long sectionMapOffset;
    private int sectionMapId;

    @Override
    public DwgVersion version() {
        return DwgVersion.R2004;
    }

    @Override
    public boolean supports(DwgVersion version) {
        // Spec chapters 6-8: R2010, R2013 and R2018 have the file structure of R2004.
        return version == DwgVersion.R2004
            || version == DwgVersion.R2010
            || version == DwgVersion.R2013
            || version == DwgVersion.R2018;
    }

    // -------------------------------------------------------------------------
    // readHeader (spec §4.1)
    // -------------------------------------------------------------------------

    private static final int META_DATA_SIZE = 0x80;
    private static final int ENCRYPTED_SIZE = 0x6C;

    /**
     * Reads the R2004 file header (spec §4.1). Plain part:
     * <pre>
     *   0x00  6  version id      0x0B 1 maintenance version   0x0D 4 preview address
     *   0x13  2  code page       0x18 4 security flags         0x20 4 summary info address
     *   0x24  4  VBA project address
     * </pre>
     * The 0x6C bytes at 0x80 are encrypted; decrypted they hold, among others, the section page
     * map address at 0x54 (add 0x100) and the section map id at 0x5C.
     */
    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        byte[] plain = new byte[META_DATA_SIZE];
        for (int i = 0; i < plain.length; i++) {
            plain[i] = (byte) input.readRawChar();
        }
        String versionId = new String(plain, 0, 6, StandardCharsets.US_ASCII);
        if (!versionId.equals("AC1018") && !versionId.equals("AC1024")
                && !versionId.equals("AC1027") && !versionId.equals("AC1032")) {
            throw new IllegalArgumentException("Invalid version string for R2004 family: " + versionId);
        }

        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2004);
        fields.setMaintenanceVersion(plain[0x0B] & 0xFF);
        fields.setPreviewOffset(ByteUtils.readLE32(plain, 0x0D) & 0xFFFFFFFFL);
        fields.setCodePage(ByteUtils.readLE16(plain, 0x13) & 0xFFFF);
        fields.setSecurityFlags((int) ByteUtils.readLE32(plain, 0x18));
        fields.setSummaryInfoOffset(ByteUtils.readLE32(plain, 0x20) & 0xFFFFFFFFL);
        fields.setVbaProjectOffset(ByteUtils.readLE32(plain, 0x24) & 0xFFFFFFFFL);

        byte[] encrypted = new byte[ENCRYPTED_SIZE];
        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] = (byte) input.readRawChar();
        }
        byte[] header = xorWithMagicSequence(encrypted);
        sectionMapOffset = ByteUtils.readLE64(header, 0x54);
        sectionMapId = (int) ByteUtils.readLE32(header, 0x5C);
        return fields;
    }

    /**
     * Decrypts the encrypted file header (spec §4.1): each byte is XORed with the next value of
     * the sequence {@code seed = seed * 0x343FD + 0x269EC3} (32-bit, starting at 1), taking
     * bits 16–23 of the seed.
     */
    static byte[] xorWithMagicSequence(byte[] data) {
        byte[] out = new byte[data.length];
        int seed = 1;
        for (int i = 0; i < data.length; i++) {
            seed = seed * 0x343FD + 0x269EC3;
            out[i] = (byte) (data[i] ^ (seed >>> 16));
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // readSections
    // -------------------------------------------------------------------------
    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)
            throws Exception {
        try {
            return readSectionsFromMap(input);
        } catch (io.dwg.core.io.ReadLimits.LimitExceededException limit) {
            throw limit;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static final int PAGE_MAP_TYPE = 0x41630e3b;
    private static final int SECTION_MAP_TYPE = 0x4163003b;
    private static final int DATA_PAGE_TYPE = 0x4163043b;

    /**
     * Sections as the file describes them (specification, "R2004 file format") :
     * the section page map gives the file address of every page, the section map gives for every
     * section its name, its size and its pages, and each data page is decompressed to its start offset
     * within the section.
     */
    private Map<String, SectionInputStream> readSectionsFromMap(BitInput input) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();
        byte[] file = readAll(input);

        // section page map : { page number, size }; the pages follow each other from 0x100
        byte[] pageMap = readSystemPage(file, sectionMapOffset + 0x100, PAGE_MAP_TYPE);
        Map<Integer, Long> pageAddress = new HashMap<>();
        long address = 0x100;
        for (int p = 0; p + 8 <= pageMap.length; ) {
            int number = le32(pageMap, p);
            int size = le32(pageMap, p + 4);
            p += 8;
            if (number < 0) {
                p += 16;                        // a gap : parent, left, right, 0
            } else {
                pageAddress.put(number, address);
            }
            if (size <= 0) {
                break;
            }
            address += size;
        }

        Long mapAddress = pageAddress.get(sectionMapId);
        if (mapAddress == null) {
            throw new IllegalStateException("section map page not found");
        }
        byte[] map = readSystemPage(file, mapAddress, SECTION_MAP_TYPE);

        // section map : 20 bytes of header, then the descriptions
        int numDescriptions = le32(map, 0);
        int p = 20;
        for (int i = 0; i < numDescriptions && p + 0x60 <= map.length; i++) {
            long sectionSize = le64(map, p);
            int pageCount = le32(map, p + 0x08);
            int maxDecompressed = le32(map, p + 0x0C);
            int compressed = le32(map, p + 0x14);       // 1 = stored, 2 = compressed
            int nameEnd = p + 0x20;
            while (nameEnd < p + 0x60 && map[nameEnd] != 0) {
                nameEnd++;
            }
            String name = new String(map, p + 0x20, nameEnd - (p + 0x20), StandardCharsets.US_ASCII);
            p += 0x60;
            if (pageCount < 0 || pageCount > 1_000_000 || p + 16L * pageCount > map.length) {
                throw new IllegalStateException("bad page count in the section map");
            }

            // the section is as long as it says, or as far as its last page reaches
            long size = sectionSize;
            io.dwg.core.io.ReadLimits.reserve((long) (pageCount) * 16L, "R2004FileStructureHandler");
            long[][] pages = new long[pageCount][];
            for (int k = 0; k < pageCount; k++) {
                pages[k] = new long[] { le32(map, p), le32(map, p + 4), le64(map, p + 8) };
                size = Math.max(size, pages[k][2] + maxDecompressed);
                p += 16;
            }
            if (name.isEmpty() || pageCount == 0 || size <= 0 || size > Integer.MAX_VALUE - 16) {
                continue;
            }

            io.dwg.core.io.ReadLimits.reserve((long) (size), "R2004FileStructureHandler");
            byte[] out = new byte[(int) size];
            for (long[] page : pages) {
                Long pageAt = pageAddress.get((int) page[0]);
                if (pageAt == null || pageAt + 32 > file.length) {
                    continue;
                }
                // the 32 byte header of a data page is XORed with a mask that depends on its address
                int at = (int) (long) pageAt;
                int mask = 0x4164536b ^ at;
                int[] h = new int[8];
                for (int k = 0; k < 8; k++) {
                    h[k] = le32(file, at + 4 * k) ^ mask;
                }
                if (h[0] != DATA_PAGE_TYPE) {
                    continue;
                }
                int dataSize = h[2];
                long startOffset = (h[4] & 0xFFFFFFFFL) | ((long) h[5] << 32);
                if (dataSize <= 0 || at + 32L + dataSize > file.length || startOffset < 0 || startOffset >= out.length) {
                    continue;
                }
                io.dwg.core.io.ReadLimits.reserve((long) (dataSize), "R2004FileStructureHandler");
                byte[] data = new byte[dataSize];
                System.arraycopy(file, at + 32, data, 0, dataSize);
                byte[] plain = data;
                if (compressed == 2) {
                    plain = new io.dwg.core.util.R2004Lz77Decompressor().decompress(data, maxDecompressed);
                }
                int n = (int) Math.min(Math.min(plain.length, maxDecompressed), out.length - startOffset);
                System.arraycopy(plain, 0, out, (int) startOffset, n);
            }
            sections.put(name, new SectionInputStream(out, name));
        }
        return sections;
    }

    // a system page : type, decompressed size, compressed size, compression type, checksum (RL each), then data
    private static byte[] readSystemPage(byte[] file, long address, int expectedType) throws Exception {
        if (address < 0 || address + 20 > file.length) {
            throw new IllegalStateException("system page outside the file");
        }
        int at = (int) address;
        if (le32(file, at) != expectedType) {
            throw new IllegalStateException("unexpected system page type");
        }
        int decompressedSize = le32(file, at + 4);
        int compressedSize = le32(file, at + 8);
        int compressionType = le32(file, at + 12);
        if (decompressedSize < 0 || compressedSize < 0 || at + 20L + compressedSize > file.length) {
            throw new IllegalStateException("bad system page size");
        }
        io.dwg.core.io.ReadLimits.reserve((long) (compressedSize), "R2004FileStructureHandler");
        byte[] data = new byte[compressedSize];
        System.arraycopy(file, at + 20, data, 0, compressedSize);
        if (compressionType != 2) {
            return data;
        }
        byte[] plain = new io.dwg.core.util.R2004Lz77Decompressor().decompress(data, decompressedSize);
        return plain.length == decompressedSize ? plain : java.util.Arrays.copyOf(plain, decompressedSize);
    }

    private static byte[] readAll(BitInput input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        input.seek(0);
        try {
            while (!input.isEof()) {
                bytes.write(input.readRawChar() & 0xFF);
            }
        } catch (io.dwg.core.io.ReadLimits.LimitExceededException limit) {
            throw limit;
        } catch (RuntimeException e) {
            // end of the input
        }
        return bytes.toByteArray();
    }

    private static int le32(byte[] b, int off) {
        return ByteBuffer.wrap(b, off, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static long le64(byte[] b, int off) {
        return ByteBuffer.wrap(b, off, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    // -------------------------------------------------------------------------
    // writeHeader / writeSections
    // -------------------------------------------------------------------------
    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // 1. Version string (6 bytes) - "AC1018"
        byte[] versionStr = "AC1018".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (byte b : versionStr) {
            output.writeRawChar(b & 0xFF);
        }

        // 2. Live data fields (0x06-0x7A, 0x7A bytes)
        byte[] liveData = new byte[0x7A];
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

        // 0x54-0x5B: Section map offset (8 bytes, LE64)
        long sectionMapOffset = header.sectionMapOffset();
        writeLE64(data, 0x54, sectionMapOffset);

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
     */
    /** Standard CRC-32 (reflected, initial value and final XOR 0xFFFFFFFF), used by the writer. */
    private int calculateCrc32(byte[] data, int offset, int length) {
        int crc = 0xFFFFFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc = CrcLookupTables.CRC32_TABLE[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
        }
        return ~crc;
    }

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
        long currentOffset = 0x100 / 0x100;
        java.util.Map<String, Long> sectionOffsets = new java.util.HashMap<>();
        java.util.Map<String, Long> sectionSizes = new java.util.HashMap<>();

        for (String sectionName : sections.keySet()) {
            byte[] sectionData = sections.get(sectionName);
            if (sectionData != null && sectionData.length > 0) {
                sectionOffsets.put(sectionName, currentOffset);
                sectionSizes.put(sectionName, (long)sectionData.length);

                for (byte b : sectionData) {
                    output.writeRawChar(b & 0xFF);
                }

                currentOffset += sectionData.length;
            }
        }

        long sectionMapStartOffset = currentOffset;

        int sectionCount = 0;
        for (String sectionName : sections.keySet()) {
            if (sections.get(sectionName) != null && sections.get(sectionName).length > 0) {
                sectionCount++;
            }
        }
        output.writeRawLong(sectionCount);

        for (String sectionName : sections.keySet()) {
            byte[] sectionData = sections.get(sectionName);
            if (sectionData != null && sectionData.length > 0) {
                writeR2004DataSectionDescriptor(output, sectionName, sectionData,
                    sectionOffsets.get(sectionName));
            }
        }
    }

    /**
     * R2004 Data Section Descriptor 쓰기
     */
    private void writeR2004DataSectionDescriptor(BitOutput output, String sectionName,
            byte[] sectionData, long sectionStartOffset) throws Exception {

        int compressedSize = sectionData.length;
        int uncompressedSize = sectionData.length;
        int compressionType = 0;  // No compression

        output.writeRawLong(compressedSize);
        output.writeRawLong(uncompressedSize);
        output.writeRawLong(compressionType);

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

        int pageCount = 1;
        output.writeRawLong(pageCount);

        output.writeRawLong(0);
        output.writeRawLong(sectionData.length);
        output.writeRawLong((int)(sectionStartOffset & 0xFFFFFFFFL));
    }


}
