package io.dwg.core.util;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;

import java.nio.ByteBuffer;

/**
 * R2004 LZ77 섹션 데이터 디컴프레싱
 * LibreDWG decompress_R2004_section() 기반 구현
 */
public class R2004Lz77Decompressor {

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        ByteBufferBitInput src = new ByteBufferBitInput(ByteBuffer.wrap(compressed));

        int outPos = 0;

        // Read first opcode
        int opcode1 = readRC(src) & 0xFF;
        System.out.printf("[DEBUG] R2004Lz77: Initial opcode=0x%02X\n", opcode1);

        // If (opcode & 0xF0) == 0, it's a literal run
        if ((opcode1 & 0xF0) == 0) {
            int litLength = readLiteralLength(src, opcode1);
            System.out.printf("[DEBUG] R2004Lz77: Initial literal run, length=%d\n", litLength);
            outPos += copyBytes(src, litLength, output, outPos);
            opcode1 = readRC(src) & 0xFF;
            System.out.printf("[DEBUG] R2004Lz77: Next opcode after literal=0x%02X\n", opcode1);
        }

        // Main decompression loop
        int iterations = 0;
        while (src.position() < compressed.length * 8 && outPos < expectedSize && opcode1 != 0x11) {
            iterations++;
            if (iterations > 1000) {
                System.out.printf("[WARN] R2004Lz77: Too many iterations, breaking\n");
                break;
            }
            int compBytes = 0;
            int compOffset = 0;

            if (opcode1 < 0x10 || opcode1 >= 0x40) {
                // Single byte offset method (0x00-0x0F or 0x40-0xFF)
                compBytes = ((opcode1 >> 4) & 0xF) - 1;
                int opcode2 = readRC(src) & 0xFF;
                compOffset = (((opcode1 >> 2) & 3) | (opcode2 << 2)) + 1;
                System.out.printf("[DEBUG] R2004Lz77: Mode 1 (0x%02X): bytes=%d offset=%d\n", opcode1, compBytes, compOffset);
            } else if (opcode1 >= 0x12 && opcode1 < 0x20) {
                // 0x12-0x1F: Two-byte offset method
                compBytes = readCompressedBytes(src, opcode1, 7);
                int offsetBase = (opcode1 & 8) << 11;
                int[] offsetRef = new int[]{offsetBase};
                opcode1 = twoByteOffset(src, 0x4000, offsetRef);
                compOffset = offsetRef[0];
                System.out.printf("[DEBUG] R2004Lz77: Mode 2 (0x%02X): bytes=%d offset=%d next=0x%02X\n", opcode1, compBytes, compOffset, opcode1);
            } else if (opcode1 >= 0x20) {
                // 0x20+: Two-byte offset method
                compBytes = readCompressedBytes(src, opcode1, 0x1f);
                int[] offsetRef = new int[]{0};
                opcode1 = twoByteOffset(src, 1, offsetRef);
                compOffset = offsetRef[0];
                System.out.printf("[DEBUG] R2004Lz77: Mode 3 (0x%02X): bytes=%d offset=%d next=0x%02X\n", opcode1, compBytes, compOffset, opcode1);
            } else if (opcode1 == 0x11) {
                System.out.printf("[DEBUG] R2004Lz77: Terminator (0x11) at outPos=%d\n", outPos);
                break;
            }

            // Copy referenced bytes (with proper LZ77 run-length extension)
            if (compBytes > 0 && compOffset > 0 && compOffset <= outPos) {
                int pos = outPos;
                int end = Math.min(pos + compBytes, expectedSize);
                int refPos = pos - compOffset;

                while (pos < end && refPos >= 0) {
                    output[pos] = output[refPos];
                    pos++;
                    refPos++;
                }
                outPos = pos;
            }

            // Handle literal data suffix
            int litLength = opcode1 & 3;
            if (litLength == 0) {
                if (src.position() >= compressed.length * 8) {
                    System.out.printf("[DEBUG] R2004Lz77: EOF reached at outPos=%d\n", outPos);
                    break;
                }
                opcode1 = readRC(src) & 0xFF;
                System.out.printf("[DEBUG] R2004Lz77: Read next opcode=0x%02X for literal check\n", opcode1);
                if ((opcode1 & 0xF0) == 0) {
                    litLength = readLiteralLength(src, opcode1);
                    System.out.printf("[DEBUG] R2004Lz77: Extended literal length=%d\n", litLength);
                }
            }

            if (litLength > 0) {
                if (outPos + litLength <= expectedSize) {
                    System.out.printf("[DEBUG] R2004Lz77: Copying %d literal bytes at outPos=%d\n", litLength, outPos);
                    outPos += copyBytes(src, litLength, output, outPos);
                    if (src.position() < compressed.length * 8) {
                        opcode1 = readRC(src) & 0xFF;
                        System.out.printf("[DEBUG] R2004Lz77: Next opcode=0x%02X\n", opcode1);
                    } else {
                        System.out.printf("[DEBUG] R2004Lz77: EOF after literals at outPos=%d\n", outPos);
                        break;
                    }
                } else {
                    System.out.printf("[DEBUG] R2004Lz77: Literal overflow, breaking at outPos=%d\n", outPos);
                    break;
                }
            }
        }

        // Return only the valid decompressed portion
        byte[] result = new byte[outPos];
        System.arraycopy(output, 0, result, 0, outPos);
        return result;
    }

    private int readRC(BitInput input) {
        return input.readRawChar() & 0xFF;
    }

    private int readLiteralLength(BitInput input, int opcode) {
        int lowbits = opcode & 0xF;
        if (lowbits == 0) {
            int lastbyte;
            while ((lastbyte = readRC(input)) == 0) {
                lowbits += 0xFF;
            }
            lowbits += 0xF + lastbyte;
        }
        return lowbits + 3;
    }

    private int readCompressedBytes(BitInput input, int opcode, int bits) {
        int compressedBytes = opcode & bits;
        if (compressedBytes == 0) {
            int lastbyte;
            while ((lastbyte = readRC(input)) == 0) {
                compressedBytes += 0xFF;
            }
            compressedBytes += lastbyte + bits;
        }
        return compressedBytes + 2;
    }

    private int twoByteOffset(BitInput input, int plus, int[] offsetRef) {
        int firstByte = readRC(input);
        int secondByte = readRC(input);
        offsetRef[0] = ((firstByte >> 2) & 0xFF) | ((secondByte & 0xFF) << 6);
        offsetRef[0] += plus;
        return firstByte;
    }

    private int copyBytes(BitInput input, int count, byte[] output, int outPos) {
        int copied = 0;
        for (int i = 0; i < count && outPos + i < output.length; i++) {
            output[outPos + i] = (byte) readRC(input);
            copied++;
        }
        return copied;
    }
}
