package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r13.R13FileStructureHandler;
import io.dwg.format.r13.R13SectionLocator;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * R13 파일 헤더 디버그
 */
public class R13HeaderDebugTest {

    public static void main(String[] args) throws Exception {
        String filePath = "samples/example_r13.dwg";
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

        System.out.println("===== R13 Header Debug =====");
        System.out.printf("File size: %d bytes (0x%X)\n\n", fileBytes.length, fileBytes.length);

        // 헤더 분석
        System.out.println("Raw bytes:");
        for (int i = 0; i < Math.min(64, fileBytes.length); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < fileBytes.length; j++) {
                System.out.printf("%02X ", fileBytes[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nParsing header:");

        // 버전 문자열
        String version = new String(fileBytes, 0, 6);
        System.out.printf("  Version string: '%s'\n", version);

        // 예약 영역
        System.out.printf("  Reserved: %02X %02X %02X %02X %02X %02X\n",
            fileBytes[6] & 0xFF, fileBytes[7] & 0xFF, fileBytes[8] & 0xFF,
            fileBytes[9] & 0xFF, fileBytes[10] & 0xFF, fileBytes[11] & 0xFF);

        // 코드페이지 (little-endian short)
        int codePage = (fileBytes[12] & 0xFF) | ((fileBytes[13] & 0xFF) << 8);
        System.out.printf("  Code page: 0x%04X (%d)\n", codePage, codePage);

        // 섹션 개수 (little-endian int)
        int sectionCount = (fileBytes[14] & 0xFF) | ((fileBytes[15] & 0xFF) << 8) |
                          ((fileBytes[16] & 0xFF) << 16) | ((fileBytes[17] & 0xFF) << 24);
        System.out.printf("  Section count: %d\n\n", sectionCount);

        // 섹션 locator들
        System.out.println("Section locators:");
        int offset = 18;
        for (int i = 0; i < sectionCount && offset + 12 <= fileBytes.length; i++) {
            int recordNumber = (fileBytes[offset] & 0xFF) | ((fileBytes[offset+1] & 0xFF) << 8) |
                              ((fileBytes[offset+2] & 0xFF) << 16) | ((fileBytes[offset+3] & 0xFF) << 24);

            long seeker = ((long)(fileBytes[offset+4] & 0xFF)) |
                         (((long)(fileBytes[offset+5] & 0xFF)) << 8) |
                         (((long)(fileBytes[offset+6] & 0xFF)) << 16) |
                         (((long)(fileBytes[offset+7] & 0xFF)) << 24);

            long size = ((long)(fileBytes[offset+8] & 0xFF)) |
                       (((long)(fileBytes[offset+9] & 0xFF)) << 8) |
                       (((long)(fileBytes[offset+10] & 0xFF)) << 16) |
                       (((long)(fileBytes[offset+11] & 0xFF)) << 24);

            System.out.printf("  [%d] Record=%d, Offset=0x%X (%d), Size=0x%X (%d)\n",
                i, recordNumber, seeker, seeker, size, size);

            offset += 12;
        }

        System.out.println("\n===== Manual Structure Analysis =====");
        int pos = 0;
        System.out.printf("[0x%02X] Version: %s\n", pos, new String(fileBytes, pos, 6));
        pos += 6;

        System.out.printf("[0x%02X] Reserved: %02X %02X %02X %02X %02X %02X\n", pos,
            fileBytes[pos], fileBytes[pos+1], fileBytes[pos+2],
            fileBytes[pos+3], fileBytes[pos+4], fileBytes[pos+5]);
        pos += 6;

        // R13 헤더는 0x0C부터 시작
        int val = fileBytes[pos] & 0xFF | ((fileBytes[pos+1] & 0xFF) << 8) |
                  ((fileBytes[pos+2] & 0xFF) << 16) | ((fileBytes[pos+3] & 0xFF) << 24);
        System.out.printf("[0x%02X] Field (RL): 0x%08X\n", pos, val);
        pos += 4;

        int val2 = (fileBytes[pos] & 0xFF) | ((fileBytes[pos+1] & 0xFF) << 8);
        System.out.printf("[0x%02X] CodePage (RS): 0x%04X\n", pos, val2);
        pos += 2;

        int val3 = fileBytes[pos] & 0xFF | ((fileBytes[pos+1] & 0xFF) << 8) |
                   ((fileBytes[pos+2] & 0xFF) << 16) | ((fileBytes[pos+3] & 0xFF) << 24);
        System.out.printf("[0x%02X] Sections (RL): %d (0x%08X)\n", pos, val3, val3);
        pos += 4;

        System.out.println("\n===== Actual Parsing =====");
        try {
            BitInput input = new ByteBufferBitInput(fileBytes);
            R13FileStructureHandler handler = new R13FileStructureHandler();
            var header = handler.readHeader(input);

            System.out.printf("Header parsed successfully\n");
            System.out.printf("  Version: %s\n", header.version());
            System.out.printf("  Maintenance version: %d\n", header.maintenanceVersion());
            System.out.printf("  Code page: 0x%04X\n", header.codePage());
            System.out.printf("  Section offsets: %s\n",
                header.sectionOffsets() == null ? "null" : header.sectionOffsets());

        } catch (Exception e) {
            System.err.printf("Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
