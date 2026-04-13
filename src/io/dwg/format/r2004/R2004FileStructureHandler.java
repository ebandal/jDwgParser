package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §4 (R2004 DWG FILE FORMAT ORGANIZATION) 구현
 */
public class R2004FileStructureHandler extends AbstractFileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R2004;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2000 || version == DwgVersion.R2004;
    }

    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2004);

        // 파일 헤더 0x80바이트 읽기
        byte[] headerBytes = new byte[0x80];
        for (int i = 0; i < 0x80; i++) {
            headerBytes[i] = (byte)input.readRawChar();
        }

        // XOR 복호화 (key=0x4844569C for R2004)
        byte[] decrypted = decryptHeader(headerBytes);

        // CRC 검증
        int expectedCrc = ByteOrderHelper.readLittleEndianInt(decrypted, 0x7C);
        validateCrc(decrypted, expectedCrc, "R2004 File Header CRC");

        // 헤더 필드 추출
        String versionStr = new String(decrypted, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        // Version: versionStr
        fields.setMaintenanceVersion(decrypted[10] & 0xFF);

        int previewOffset = ByteOrderHelper.readLittleEndianInt(decrypted, 12);
        fields.setPreviewOffset(previewOffset);

        int codePage = ByteOrderHelper.readLittleEndianShort(decrypted, 24) & 0xFFFF;
        fields.setCodePage(codePage);

        int securityFlags = ByteOrderHelper.readLittleEndianInt(decrypted, 28);
        fields.setSecurityFlags(securityFlags);

        int summaryOffset = ByteOrderHelper.readLittleEndianInt(decrypted, 36);
        fields.setSummaryInfoOffset(summaryOffset);

        long vbaOffset = ByteOrderHelper.readLittleEndianLong(decrypted, 40) & 0xFFFFFFFFL;
        fields.setVbaProjectOffset(vbaOffset);

        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();
        
        // R2004 section map 구조를 읽어서 섹션을 조합해야 함
        // TODO: Implement section map reading and assembly
        
        return sections;
    }

    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // TODO: Implement header writing for R2004
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception {
        // TODO: Implement section writing for R2004
    }

    /**
     * R2004 헤더 XOR 복호화
     */
    private byte[] decryptHeader(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];
        int[] magicNumber = generateMagicNumber();

        for (int i = 0; i < encrypted.length; i++) {
            decrypted[i] = (byte)(encrypted[i] ^ magicNumber[i]);
        }

        return decrypted;
    }

    /**
     * R2004 Magic Number 생성
     */
    private int[] generateMagicNumber() {
        int[] magicNumber = new int[0x6C];
        int randseed = 1;

        for (int i = 0; i < magicNumber.length; i++) {
            randseed *= 0x0343FD;
            randseed += 0x269EC3;
            magicNumber[i] = (randseed >> 16) & 0xFF;
        }

        return magicNumber;
    }

    /**
     * Helper class for byte order operations
     */
    private static class ByteOrderHelper {
        static int readLittleEndianInt(byte[] data, int offset) {
            return (data[offset] & 0xFF) |
                   ((data[offset + 1] & 0xFF) << 8) |
                   ((data[offset + 2] & 0xFF) << 16) |
                   ((data[offset + 3] & 0xFF) << 24);
        }

        static short readLittleEndianShort(byte[] data, int offset) {
            return (short)((data[offset] & 0xFF) |
                          ((data[offset + 1] & 0xFF) << 8));
        }

        static long readLittleEndianLong(byte[] data, int offset) {
            return (long)(readLittleEndianInt(data, offset)) & 0xFFFFFFFFL |
                   (((long)readLittleEndianInt(data, offset + 4)) & 0xFFFFFFFFL) << 32;
        }
    }
}
