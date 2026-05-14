package debug;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Compare R2000 and R2004 file headers byte-by-byte
 */
public class CompareR2000R2004Headers {
    public static void main(String[] args) throws Exception {
        byte[] r2000Data = Files.readAllBytes(Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg"));
        byte[] r2004Data = Files.readAllBytes(Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2004\\Arc.dwg"));

        int maxLen = Math.min(r2000Data.length, r2004Data.length);
        maxLen = Math.min(maxLen, 256);

        System.out.println("=== R2000 vs R2004 Header Comparison ===\n");
        System.out.println("Byte | R2000   R2004   | ASCII Char");
        System.out.println("-----|-------------|");

        int firstDiff = -1;
        for (int i = 0; i < maxLen; i++) {
            byte r2000 = r2000Data[i];
            byte r2004 = r2004Data[i];

            String r2000Hex = String.format("%02X", r2000 & 0xFF);
            String r2004Hex = String.format("%02X", r2004 & 0xFF);

            String marker = r2000 == r2004 ? "  " : "==";

            char r2000Char = (r2000 >= 32 && r2000 < 127) ? (char)r2000 : '.';
            char r2004Char = (r2004 >= 32 && r2004 < 127) ? (char)r2004 : '.';

            System.out.printf("0x%02X | %s  %s  %s | %c  %c\n", i, r2000Hex, r2004Hex, marker, r2000Char, r2004Char);

            if (firstDiff == -1 && r2000 != r2004) {
                firstDiff = i;
            }
        }

        System.out.println("\nFirst difference at byte 0x" + String.format("%02X", firstDiff));

        // Now let's interpret the headers
        System.out.println("\n=== R2000 Header Structure Interpretation ===");
        interpretR2000Header(r2000Data);

        System.out.println("\n=== R2004 Header Structure Interpretation ===");
        interpretR2004Header(r2004Data);
    }

    static void interpretR2000Header(byte[] data) {
        int pos = 0;
        System.out.printf("0x%02X-0x%02X: Version string: %s\n", pos, pos+5,
            new String(data, pos, 6, java.nio.charset.StandardCharsets.US_ASCII));
        pos += 6;

        System.out.printf("0x%02X-0x%02X: Reserved: ", pos, pos+5);
        for (int i = 0; i < 6; i++) {
            System.out.printf("%02X ", data[pos+i] & 0xFF);
        }
        System.out.println();
        pos += 6;

        System.out.printf("0x%02X: unknown_0: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        int previewAddr = ((data[pos] & 0xFF) |
                          ((data[pos+1] & 0xFF) << 8) |
                          ((data[pos+2] & 0xFF) << 16) |
                          ((data[pos+3] & 0xFF) << 24));
        System.out.printf("0x%02X-0x%02X: preview_address (RL): 0x%08X (%d)\n", pos, pos+3, previewAddr, previewAddr);
        pos += 4;

        System.out.printf("0x%02X: dwg_version: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        System.out.printf("0x%02X: maint_version: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        short codepage = (short)(data[pos] & 0xFF | ((data[pos+1] & 0xFF) << 8));
        System.out.printf("0x%02X-0x%02X: codepage (RS): 0x%04X (%d)\n", pos, pos+1, codepage & 0xFFFF, codepage);
        pos += 2;

        System.out.printf("0x%02X: section_count: 0x%02X (%d)\n", pos, data[pos] & 0xFF, data[pos] & 0xFF);
        int sectionCount = data[pos] & 0xFF;
        pos += 1;

        System.out.println("\nR2000 assumes " + sectionCount + " section locators at 0x" + String.format("%02X", pos));
    }

    static void interpretR2004Header(byte[] data) {
        int pos = 0;
        System.out.printf("0x%02X-0x%02X: Version string: %s\n", pos, pos+5,
            new String(data, pos, 6, java.nio.charset.StandardCharsets.US_ASCII));
        pos += 6;

        System.out.printf("0x%02X-0x%02X: Reserved: ", pos, pos+5);
        for (int i = 0; i < 6; i++) {
            System.out.printf("%02X ", data[pos+i] & 0xFF);
        }
        System.out.println();
        pos += 6;

        System.out.printf("0x%02X: unknown_0: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        int previewAddr = ((data[pos] & 0xFF) |
                          ((data[pos+1] & 0xFF) << 8) |
                          ((data[pos+2] & 0xFF) << 16) |
                          ((data[pos+3] & 0xFF) << 24));
        System.out.printf("0x%02X-0x%02X: preview_address (RL): 0x%08X (%d)\n", pos, pos+3, previewAddr, previewAddr);
        pos += 4;

        System.out.printf("0x%02X: dwg_version: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        System.out.printf("0x%02X: maint_version: 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        short codepage = (short)(data[pos] & 0xFF | ((data[pos+1] & 0xFF) << 8));
        System.out.printf("0x%02X-0x%02X: codepage (RS): 0x%04X (%d)\n", pos, pos+1, codepage & 0xFFFF, codepage);
        pos += 2;

        System.out.printf("0x%02X: section_count(?): 0x%02X\n", pos, data[pos] & 0xFF);
        pos += 1;

        System.out.println("\nR2004 has very different structure after codepage!");
    }
}
