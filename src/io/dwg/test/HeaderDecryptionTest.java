package io.dwg.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * DWG 헤더 복호화 로직 검증
 * R2004/R2007 XOR 복호화 및 필드 파싱 검증
 */
public class HeaderDecryptionTest {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("사용법: java io.dwg.test.HeaderDecryptionTest <dwg_file>");
            return;
        }

        String filePath = args[0];
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  DWG 헤더 복호화 검증");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("파일: " + filePath + "\n");

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

            // 버전 확인
            String versionStr = new String(fileBytes, 0, 6, "US-ASCII");
            System.out.printf("버전 문자열: %s\n\n", versionStr);

            // 헤더 0x80바이트 추출
            byte[] headerBytes = new byte[0x80];
            System.arraycopy(fileBytes, 0, headerBytes, 0, 0x80);

            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.println("원본 헤더 바이트 (HEX):");
            System.out.println("─────────────────────────────────────────────────────────────");
            printHexDump(headerBytes, 0, Math.min(0x40, headerBytes.length));

            // 복호화 시뮬레이션
            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("Magic Number 생성:");
            System.out.println("─────────────────────────────────────────────────────────────");
            int[] magic = generateMagicNumber();
            System.out.print("처음 32바이트 magic: ");
            for (int i = 0; i < Math.min(32, magic.length); i++) {
                System.out.printf("%02X ", magic[i] & 0xFF);
            }
            System.out.println();

            // 복호화 실행
            byte[] decrypted = decryptHeader(headerBytes);

            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("복호화된 헤더 바이트 (HEX):");
            System.out.println("─────────────────────────────────────────────────────────────");
            printHexDump(decrypted, 0, Math.min(0x40, decrypted.length));

            // 필드 파싱
            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("헤더 필드 파싱:");
            System.out.println("─────────────────────────────────────────────────────────────");

            System.out.printf("  [0x00-0x05] 버전 문자열: %s\n",
                new String(decrypted, 0, 6, "US-ASCII"));

            System.out.printf("  [0x06-0x09] Unknown1: 0x%08X\n",
                readLE32(decrypted, 0x06));

            int maint = decrypted[10] & 0xFF;
            System.out.printf("  [0x0A] Maintenance Version: %d (0x%02X)\n", maint, maint);

            int codePage = readLE16(decrypted, 0x18);
            System.out.printf("  [0x18-0x19] CodePage: %d (0x%04X)\n", codePage, codePage);

            int securityFlags = readLE32(decrypted, 0x1C);
            System.out.printf("  [0x1C-0x1F] Security Flags: 0x%08X\n", securityFlags);

            int summaryOffset = readLE32(decrypted, 0x24);
            System.out.printf("  [0x24-0x27] Summary Info Offset: 0x%08X (%d)\n", summaryOffset, summaryOffset);

            long vbaOffset = readLE64(decrypted, 0x28);
            System.out.printf("  [0x28-0x2F] VBA Project Offset: 0x%016X (%d)\n", vbaOffset, vbaOffset);

            // R2004 전용
            if ("AC1018".equals(versionStr)) {
                System.out.println("\n  [R2004 전용 필드]");
                int sectionMapId = readLE32(decrypted, 0x48);
                System.out.printf("    [0x48-0x4B] Section Map ID: 0x%08X (%d)\n", sectionMapId, sectionMapId);

                int sectionMapOffset = readLE32(decrypted, 0x4C);
                System.out.printf("    [0x4C-0x4F] Section Map Offset: 0x%08X (%d)\n", sectionMapOffset, sectionMapOffset);

                if (sectionMapOffset > 0 && sectionMapOffset < fileBytes.length) {
                    System.out.printf("    → 유효한 offset (파일 크기: %d)\n", fileBytes.length);
                } else {
                    System.out.printf("    ⚠ 주의: 범위 초과 (파일 크기: %d)\n", fileBytes.length);
                }
            }

            // R2007 전용
            if ("AC1021".equals(versionStr)) {
                System.out.println("\n  [R2007 전용 필드]");
                long pageMapOffset = readLE64(decrypted, 0x24);
                long sectionMapId = readLE64(decrypted, 0x28);
                System.out.printf("    [0x24-0x2B] Page Map Offset: 0x%016X (%d)\n", pageMapOffset, pageMapOffset);
                System.out.printf("    [0x28-0x2F] Section Map ID: 0x%016X (%d)\n", sectionMapId, sectionMapId);
            }

            // CRC 검증 (있으면)
            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("CRC 정보:");
            System.out.println("─────────────────────────────────────────────────────────────");
            int crcOffset = readLE32(decrypted, 0x7C);
            System.out.printf("  [0x7C-0x7F] CRC (저장된 값): 0x%08X\n", crcOffset);

        } catch (Exception e) {
            System.err.printf("오류: %s\n", e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static byte[] decryptHeader(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];
        int[] magic = generateMagicNumber();
        int limit = Math.min(encrypted.length, magic.length);
        for (int i = 0; i < limit; i++) {
            decrypted[i] = (byte) (encrypted[i] ^ magic[i]);
        }
        // 나머지는 복사
        for (int i = limit; i < encrypted.length; i++) {
            decrypted[i] = encrypted[i];
        }
        return decrypted;
    }

    private static int[] generateMagicNumber() {
        int[] magic = new int[0x6C];
        int seed = 1;
        for (int i = 0; i < magic.length; i++) {
            seed = seed * 0x0343FD + 0x269EC3;
            magic[i] = (seed >> 16) & 0xFF;
        }
        return magic;
    }

    private static int readLE32(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static int readLE16(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    private static long readLE64(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private static void printHexDump(byte[] data, int start, int length) {
        for (int i = start; i < length; i += 16) {
            System.out.printf("  %04X: ", i);
            for (int j = 0; j < 16 && (i + j) < length; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
