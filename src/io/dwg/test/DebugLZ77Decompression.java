package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug LZ77 decompression step by step
 */
public class DebugLZ77Decompression {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("RS decoder failed");
            return;
        }

        int comprLen = readLE32(decoded, 24);
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging LZ77 Decompression");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Extract compressed data
        byte[] compressed = new byte[comprLen];
        System.arraycopy(decoded, 32, compressed, 0, comprLen);

        System.out.println("Compressed data: " + compressed.length + " bytes");
        System.out.println("First 32 bytes:");
        for (int i = 0; i < 32; i++) {
            System.out.printf("%02X ", compressed[i] & 0xFF);
        }
        System.out.println("\n");

        // Step through decompression manually
        int srcPos = 0;
        int outPos = 0;
        int opcode = compressed[srcPos++] & 0xFF;

        System.out.printf("Initial opcode: 0x%02X (%d)\n", opcode, opcode);
        System.out.printf("  Binary: %s\n", Integer.toBinaryString(opcode));

        // Check for literal header
        if ((opcode & 0xF0) == 0x20) {
            System.out.println("  → Header literal (0x20 series)");
            srcPos += 2;
            int literalLength = compressed[srcPos++] & 0x07;
            System.out.printf("    Literal length: %d\n", literalLength);
            outPos += literalLength;
            System.out.printf("    Out position after header: %d\n\n", outPos);
        } else {
            System.out.println("  → NOT a header literal\n");
        }

        // Read first literal run
        System.out.printf("Current srcPos: %d (0x%02X)\n", srcPos, srcPos);
        System.out.printf("Current outPos: %d\n", outPos);

        int literalLength = readLiteralLength(compressed, srcPos, opcode);
        System.out.printf("Literal length from opcode 0x%02X: %d\n", opcode, literalLength);

        srcPos++;

        System.out.printf("After literal opcode, srcPos: %d\n", srcPos);
        System.out.printf("Copying %d literal bytes\n", literalLength);
        outPos += literalLength;
        srcPos += literalLength;

        System.out.printf("After copying literals, srcPos: %d, outPos: %d\n\n", srcPos, outPos);

        // Read next opcode and instruction
        if (srcPos < compressed.length) {
            opcode = compressed[srcPos++] & 0xFF;
            System.out.printf("Next opcode: 0x%02X (case %d)\n", opcode, opcode >> 4);

            int[] instr = readInstructions(compressed, srcPos, opcode);
            int offset = instr[0];
            int length = instr[1];
            srcPos = instr[3];

            System.out.printf("  Instruction: offset=%d (0x%X), length=%d\n", offset, offset, length);
            System.out.printf("  Current outPos: %d\n", outPos);
            System.out.printf("  Reference position: %d - %d = %d\n", outPos, offset, outPos - offset);

            if (outPos - offset < 0) {
                System.out.println("  ❌ ERROR: Negative reference position!");
                System.out.println("  This means the offset is larger than the output position");
                System.out.println("  This could indicate an error in instruction decoding");
            } else {
                System.out.println("  ✅ Valid reference");
                outPos += length;
            }

            System.out.printf("\nAfter back-reference, outPos: %d, srcPos: %d\n", outPos, srcPos);
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static int readLiteralLength(byte[] data, int pos, int opcode) {
        int length = opcode + 8;

        if (length == 0x17) {
            if (pos < data.length) {
                int n = data[pos] & 0xFF;
                length += n;

                if (n == 0xFF) {
                    pos++;
                    do {
                        if (pos + 1 >= data.length) break;
                        n = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
                        length += n;
                        pos += 2;
                    } while (n == 0xFFFF);
                }
            }
        }
        return length;
    }

    private static int[] readInstructions(byte[] data, int srcPos, int opcode) {
        int offset = 0;
        int length = 0;
        int nextOpcode = opcode;
        int pos = srcPos;

        int case_num = opcode >> 4;

        switch (case_num) {
        case 0:
            length = (opcode & 0x0F) + 0x13;
            if (pos < data.length) offset = data[pos++] & 0xFF;
            if (pos < data.length) {
                nextOpcode = data[pos++] & 0xFF;
                length = length + ((nextOpcode >> 3) & 0x10);
                offset = offset + (((nextOpcode & 0x78) << 5) + 1);
            }
            break;

        case 1:
            length = (opcode & 0x0F) + 3;
            if (pos < data.length) offset = data[pos++] & 0xFF;
            if (pos < data.length) {
                nextOpcode = data[pos++] & 0xFF;
                offset = offset + (((nextOpcode & 0xF8) << 5) + 1);
            }
            break;

        case 2:
            if (pos < data.length) offset = data[pos++] & 0xFF;
            if (pos < data.length) offset = offset | ((data[pos++] & 0xFF) << 8);
            length = opcode & 0x07;

            if ((opcode & 0x08) == 0) {
                if (pos < data.length) {
                    nextOpcode = data[pos++] & 0xFF;
                    length = length + ((nextOpcode & 0xF8));
                }
            } else {
                offset = offset + 1;
                if (pos < data.length) length = length + ((data[pos++] & 0xFF) << 3);
                if (pos < data.length) {
                    nextOpcode = data[pos++] & 0xFF;
                    length = length + ((nextOpcode & 0xF8) << 8) + 0x100;
                }
            }
            break;

        default:
            length = opcode >> 4;
            offset = opcode & 0x0F;
            if (pos < data.length) {
                nextOpcode = data[pos++] & 0xFF;
                offset = offset + (((nextOpcode & 0xF8) << 1) + 1);
            }
            break;
        }

        return new int[] { offset, length, nextOpcode, pos };
    }

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }
}
