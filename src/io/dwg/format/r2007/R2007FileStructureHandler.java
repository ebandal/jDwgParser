package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.Lz77Compressor;
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

        // R2007 header 필드를 FileHeaderFields에 저장
        fields.setPageMapOffset(h.pageMapOffset());
        fields.setPageMapSizeComp(h.pageMapSizeComp());
        fields.setPageMapSizeUncomp(h.pageMapSizeUncomp());
        fields.setSectionMapId(h.sectionsMapId());
        fields.setSectionsMapSizeComp(h.sectionsMapSizeComp());
        fields.setSectionsMapSizeUncomp(h.sectionsMapSizeUncomp());

        return fields;
    }

    // -------------------------------------------------------------------------
    // readSections
    // -------------------------------------------------------------------------
    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)
            throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        // NOTE: R2007+ structure is too complex to parse without proper RS header decoding
        // Return empty sections to avoid crashes
        if (header.pageMapOffset() == 0) return sections;

        // NOTE: R2007+ file structure is complex (RS encoding + LZ77 compression).
        // Full implementation requires correctly decoding the RS-encoded header
        // to extract exact PageMap and SectionMap offsets. For now, we return
        // empty sections to prevent crashes on R2007+ files.
        //
        // Future work:
        // 1. Fully debug RS header decoding
        // 2. Find correct PageMap/SectionMap offsets in Arc.dwg
        // 3. Implement proper section page assembly

        return sections;
    }

    private byte[] readRawBytes(BitInput input, long byteOffset, int size) throws Exception {
        input.seek(byteOffset * 8);  // Convert byte offset to bit offset
        byte[] buf = new byte[size];
        int read = 0;
        while (read < size && !input.isEof()) {
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

            byte[] pageData = readRawBytes(input, pageOffset, (int) page.dataSize());
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
        System.out.println("[DEBUG] R2007.writeSections: start");

        // Data starts at 0x480 (after header)
        long dataStartOffset = 0x480;
        long currentOffset = dataStartOffset;

        // Page IDs: 1 = section map, 2+ = section data
        int nextPageId = 2;
        java.util.Map<String, Integer> sectionPageIds = new java.util.HashMap<>();
        java.util.Map<String, Long> sectionOffsets = new java.util.HashMap<>();
        java.util.List<String> sectionOrder = new java.util.ArrayList<>(sections.keySet());

        // 1. Write section data pages and track their offsets/IDs
        System.out.println("[DEBUG] Writing section data pages...");
        for (String sectionName : sectionOrder) {
            byte[] sectionData = sections.get(sectionName);
            sectionPageIds.put(sectionName, nextPageId);
            sectionOffsets.put(sectionName, currentOffset);

            for (byte b : sectionData) {
                output.writeRawChar(b & 0xFF);
            }
            currentOffset += sectionData.length;
            nextPageId++;
        }

        // 2. Build Section Map binary structure
        System.out.println("[DEBUG] Building Section Map...");
        byte[] sectionMapBytes = buildSectionMapBytes(sections, sectionOrder, sectionPageIds, sectionOffsets);

        // 3. LZ77-compress the Section Map
        System.out.println("[DEBUG] Compressing Section Map with LZ77...");
        Lz77Compressor compressor = new Lz77Compressor();
        byte[] compressedSectionMap = compressor.compress(sectionMapBytes);

        // Write Section Map page (at page ID 1)
        long sectionMapPageOffset = currentOffset;
        // Write page header: type(2) + decompressedSize(4) + compressedSize(4) + checksum(4)
        output.writeRawShort((short)0x4163);  // type = "Ac"
        output.writeRawLong((int)(sectionMapBytes.length & 0xFFFFFFFFL));
        output.writeRawLong((int)(compressedSectionMap.length & 0xFFFFFFFFL));
        output.writeRawLong(0);  // checksum = 0 (not validated)
        for (byte b : compressedSectionMap) {
            output.writeRawChar(b & 0xFF);
        }
        currentOffset += 14 + compressedSectionMap.length;

        // 4. Build Page Map structure
        System.out.println("[DEBUG] Building Page Map...");
        byte[] pageMapBytes = buildPageMapBytes(sections, sectionOrder, sectionPageIds,
            sectionOffsets, 1, sectionMapPageOffset, compressedSectionMap.length);

        // Write Page Map (uncompressed, starting at pageMapOffset from header)
        long pageMapOffset = currentOffset;
        long pageMapPageHeader = 14;  // type(2) + decompressedSize(4) + compressedSize(4) + checksum(4)

        output.writeRawShort((short)0x4163);  // type
        output.writeRawLong((int)(pageMapBytes.length & 0xFFFFFFFFL));
        output.writeRawLong((int)(pageMapBytes.length & 0xFFFFFFFFL));  // uncompressed, so same size
        output.writeRawLong(0);  // checksum

        for (byte b : pageMapBytes) {
            output.writeRawChar(b & 0xFF);
        }
        currentOffset += pageMapPageHeader + pageMapBytes.length;

        System.out.println("[DEBUG] R2007.writeSections: complete at offset 0x" + Long.toHexString(currentOffset));
    }

    /**
     * Build Section Map binary structure for R2007
     */
    private byte[] buildSectionMapBytes(java.util.Map<String, byte[]> sections,
            java.util.List<String> sectionOrder, java.util.Map<String, Integer> pageIds,
            java.util.Map<String, Long> offsets) throws Exception {

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        // Write section count (LE32)
        writeLE32Stream(baos, sections.size());

        // Write each section descriptor
        for (String sectionName : sectionOrder) {
            byte[] sectionData = sections.get(sectionName);
            int pageId = pageIds.get(sectionName);

            // dataSize (LE64)
            writeLE64Stream(baos, sectionData.length);

            // uncompressedSize (LE64)
            writeLE64Stream(baos, sectionData.length);

            // compressionType (LE64) = 0 for no compression
            writeLE64Stream(baos, 0);

            // sectionId (LE64) = 0
            writeLE64Stream(baos, 0);

            // encrypted (LE64) = 0
            writeLE64Stream(baos, 0);

            // unknown2 (LE64) = 0
            writeLE64Stream(baos, 0);

            // sectionName (64 bytes UTF-16LE)
            byte[] nameBytes = sectionName.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
            baos.write(nameBytes);
            // Pad to 64 bytes
            for (int i = nameBytes.length; i < 64; i++) {
                baos.write(0);
            }

            // pageCount (LE32) = 1 page per section
            writeLE32Stream(baos, 1);

            // Page record: pageId (LE64) + pageSize (LE64)
            writeLE64Stream(baos, pageId);
            writeLE64Stream(baos, sectionData.length);
        }

        return baos.toByteArray();
    }

    /**
     * Build Page Map binary structure for R2007
     */
    private byte[] buildPageMapBytes(java.util.Map<String, byte[]> sections,
            java.util.List<String> sectionOrder, java.util.Map<String, Integer> sectionPageIds,
            java.util.Map<String, Long> sectionOffsets, int sectionMapPageId,
            long sectionMapPageOffset, int sectionMapPageSize) throws Exception {

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        // Write page records: pageId (LE32) + size (LE32)
        for (String sectionName : sectionOrder) {
            int pageId = sectionPageIds.get(sectionName);
            byte[] sectionData = sections.get(sectionName);

            // pageId
            writeLE32Stream(baos, pageId);

            // size
            writeLE32Stream(baos, (int)(sectionData.length & 0xFFFFFFFFL));
        }

        // Section Map page entry
        writeLE32Stream(baos, sectionMapPageId);
        writeLE32Stream(baos, sectionMapPageSize);

        return baos.toByteArray();
    }

    /**
     * Helper: Write LE32 to stream
     */
    private void writeLE32Stream(java.io.OutputStream out, int value) throws java.io.IOException {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    /**
     * Helper: Write LE64 to stream
     */
    private void writeLE64Stream(java.io.OutputStream out, long value) throws java.io.IOException {
        writeLE32Stream(out, (int)(value & 0xFFFFFFFFL));
        writeLE32Stream(out, (int)((value >>> 32) & 0xFFFFFFFFL));
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
