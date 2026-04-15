package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.PageInfo;
import io.dwg.format.common.SectionDescriptor;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §5 (R2007 DWG FILE FORMAT ORGANIZATION) 구현.
 * LZ77 압축, UTF-16 문자열, Page Map → Section Map 처리.
 */
public class R2007FileStructureHandler extends AbstractFileStructureHandler {

    @Override
    public DwgVersion version() { return DwgVersion.R2007; }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2007 || version == DwgVersion.R2010;
    }

    // -------------------------------------------------------------------------
    // readHeader
    // -------------------------------------------------------------------------
    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2007);
        R2007FileHeader h = R2007FileHeader.read(input);

        // pageMapOffset / sectionMapId는 readSections에서 사용
        // FileHeaderFields에 임시 저장 (summary용 오프셋 필드 재활용)
        fields.setSummaryInfoOffset(h.pageMapOffset());
        fields.setVbaProjectOffset(h.sectionMapId());

        return fields;
    }

    // -------------------------------------------------------------------------
    // readSections
    // -------------------------------------------------------------------------
    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)
            throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        long pageMapOffset  = header.summaryInfoOffset();
        long sectionMapId   = header.vbaProjectOffset();

        if (pageMapOffset == 0) return sections;

        Lz77Decompressor lz77 = new Lz77Decompressor();

        // ① Page Map 읽기
        R2007PageMap pageMap = readPageMap(input, pageMapOffset, lz77);

        // ② Section Map 페이지 위치 조회
        Long sectionMapPageOffset = pageMap.offsetForPage(sectionMapId).orElse(null);
        if (sectionMapPageOffset == null) return sections;

        // ③ Section Map 읽기 (LZ77 해제)
        byte[] smCompressed = readRawPage(input, sectionMapPageOffset, 0x400);
        byte[] smData = lz77.decompress(smCompressed, smCompressed.length * 2);
        R2007SectionMap sectionMap = R2007SectionMap.read(smData);

        // ④ 각 섹션 페이지 조합 + LZ77 해제
        for (SectionDescriptor desc : sectionMap.descriptors()) {
            try {
                byte[] data = assembleSectionData(input, desc, pageMap, lz77);
                sections.put(desc.name(), new SectionInputStream(data, desc.name()));
            } catch (Exception e) {
                // 섹션 실패 무시
            }
        }

        return sections;
    }

    private R2007PageMap readPageMap(BitInput input, long offset, Lz77Decompressor lz77)
            throws Exception {
        input.seek(offset * 8);
        // 페이지 맵 헤더: type(RS)+decompressedSize(RL)+compressedSize(RL)+checksum(RL)
        input.readRawShort();               // type
        long decompressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long compressedSize   = input.readRawLong() & 0xFFFFFFFFL;
        input.readRawLong();                // checksum

        byte[] compressed = new byte[(int) compressedSize];
        for (int i = 0; i < compressed.length; i++) compressed[i] = (byte) input.readRawChar();

        return R2007PageMap.read(compressed, decompressedSize);
    }

    private byte[] readRawPage(BitInput input, long byteOffset, int maxSize) throws Exception {
        input.seek(byteOffset * 8);
        byte[] buf = new byte[maxSize];
        int read = 0;
        while (read < maxSize && !input.isEof()) {
            buf[read++] = (byte) input.readRawChar();
        }
        byte[] result = new byte[read];
        System.arraycopy(buf, 0, result, 0, read);
        return result;
    }

    private byte[] assembleSectionData(BitInput input, SectionDescriptor desc,
            R2007PageMap pageMap, Lz77Decompressor lz77) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (PageInfo page : desc.pages()) {
            long pageId = page.pageId();
            Long pageOffset = pageMap.offsetForPage(pageId).orElse(null);
            if (pageOffset == null) continue;

            byte[] pageData = readRawPage(input, pageOffset, (int) page.dataSize());
            baos.write(pageData);
        }

        byte[] combined = baos.toByteArray();
        if (desc.compressionType() == 2 && desc.uncompressedSize() > 0) {
            return lz77.decompress(combined, (int) desc.uncompressedSize());
        }
        return combined;
    }

    // -------------------------------------------------------------------------
    // writeHeader / writeSections (Phase 3)
    // -------------------------------------------------------------------------
    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        System.out.println("[DEBUG] R2007.writeHeader: start");

        // 1. Version string (6 bytes) - "AC1021"
        byte[] versionStr = "AC1021".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (byte b : versionStr) {
            output.writeRawChar(b & 0xFF);
        }

        // 2. Live data fields (0x06-0x39, 52 bytes of zeros)
        for (int i = 0; i < 52; i++) {
            output.writeRawChar(0);
        }

        // 3. Padding (0x3A-0x7F, 70 bytes of zeros)
        for (int i = 0; i < 70; i++) {
            output.writeRawChar(0);
        }

        // 4. Build 717-byte RS encoding payload
        byte[] payload = new byte[717];
        writeLE64(payload, 0, header.pageMapOffset());
        writeLE64(payload, 16, header.sectionMapId());
        // Rest of payload remains zero-filled

        // 5. RS-encode the payload
        byte[] rsEncoded = io.dwg.core.util.ReedSolomonEncoder.encodeR2007Data(payload);
        System.out.println("[DEBUG] RS-encoded " + payload.length + " bytes to " + rsEncoded.length + " bytes");

        // 6. Write RS-encoded header (765 bytes = 3 x 255)
        for (byte b : rsEncoded) {
            output.writeRawChar(b & 0xFF);
        }

        // 7. Padding to reach 0x480 total header size
        // Written so far: 6 + 52 + 70 + 765 = 893 bytes
        // Need to reach: 0x480 = 1152 bytes
        int padSize = 0x480 - (6 + 52 + 70 + 765);
        for (int i = 0; i < padSize; i++) {
            output.writeRawChar(0);
        }

        System.out.println("[DEBUG] R2007.writeHeader: complete, total=" + 0x480 + " bytes");
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections,
            FileHeaderFields header) throws Exception {
        // Phase 3 구현 예정
    }

    /**
     * Helper: Write 64-bit little-endian value
     */
    private void writeLE64(byte[] data, int offset, long value) {
        writeLE32(data, offset, (int)(value & 0xFFFFFFFFL));
        writeLE32(data, offset + 4, (int)((value >>> 32) & 0xFFFFFFFFL));
    }

    /**
     * Helper: Write 32-bit little-endian value
     */
    private void writeLE32(byte[] data, int offset, int value) {
        data[offset] = (byte)(value & 0xFF);
        data[offset + 1] = (byte)((value >>> 8) & 0xFF);
        data[offset + 2] = (byte)((value >>> 16) & 0xFF);
        data[offset + 3] = (byte)((value >>> 24) & 0xFF);
    }
}
