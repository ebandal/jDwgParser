package io.dwg.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test: Try decrypting SectionMap page header (R2004 style)
 * R2004 uses XOR with secMask = (0x4164536bL ^ fileOffset)
 * R2007 might use same formula or different one
 */
public class TestSectionMapDecryption {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long fileOffset = 0xFC80;
        byte[] pageHeader = new byte[32];
        System.arraycopy(fileData, (int)fileOffset, pageHeader, 0, 32);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: R2007 SectionMap page decryption");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("File offset: 0x%X\n", fileOffset);
        System.out.println("\nRaw header (first 32 bytes):");
        for (int i = 0; i < 32; i += 16) {
            System.out.printf("+%02X: ", i);
            for (int j = 0; j < 16; j++) {
                System.out.printf("%02X ", pageHeader[i + j] & 0xFF);
            }
            System.out.println();
        }

        // Try R2004-style decryption
        System.out.println("\n\nTrying R2004-style XOR decryption:\n");

        long secMask = (0x4164536bL ^ (fileOffset & 0xFFFFFFFFL));
        System.out.printf("secMask = (0x4164536bL ^ 0x%X) = 0x%X\n\n", fileOffset, secMask);

        byte[] decrypted = new byte[32];
        for (int i = 0; i < 8; i++) {
            int encrypted = ByteBuffer.wrap(pageHeader, i * 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int decryptedVal = (int)(encrypted ^ secMask);
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(decryptedVal);

            byte[] decryptedBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(decryptedVal).array();
            System.arraycopy(decryptedBytes, 0, decrypted, i * 4, 4);

            System.out.printf("LE32[+%d]: 0x%08X ^ 0x%08X = 0x%08X\n",
                i*4, encrypted, secMask, decryptedVal);
        }

        System.out.println("\nDecrypted header (LE32 chunks):");
        for (int i = 0; i < 8; i++) {
            int val = ByteBuffer.wrap(decrypted, i * 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            System.out.printf("  [+%02X] 0x%08X\n", i*4, val);
        }

        // Try interpreting as Big-Endian (R2004 style)
        System.out.println("\nDecrypted header (BE32 interpretation - R2004 style):");
        int pageType = ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        int field1 = ByteBuffer.wrap(decrypted, 4, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        int compSize = ByteBuffer.wrap(decrypted, 8, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        int decompSize = ByteBuffer.wrap(decrypted, 12, 4).order(ByteOrder.BIG_ENDIAN).getInt();

        System.out.printf("  pageType:    0x%08X\n", pageType);
        System.out.printf("  field[1]:    0x%08X\n", field1);
        System.out.printf("  compSize:    0x%08X (%d)\n", compSize, compSize);
        System.out.printf("  decompSize:  0x%08X (%d)\n", decompSize, decompSize);

        // Check if these match expected values
        System.out.println("\nValidation against header values:");
        System.out.printf("  Expected compressed:   905 bytes\n");
        System.out.printf("  Expected decompressed: 2188 bytes\n");

        if (compSize == 905 && decompSize == 2188) {
            System.out.println("\n✅ SUCCESS! Decryption matches expected values!");
        } else if (compSize > 0 && compSize < 10000 && decompSize > 0 && decompSize < 10000) {
            System.out.println("\n⚠️ PARTIAL: Values look reasonable but don't match exactly");
            System.out.printf("  Got: %d compressed, %d decompressed\n", compSize, decompSize);
        } else {
            System.out.println("\n❌ Decryption doesn't look right");
        }
    }
}
