package io.dwg.core.util;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;

import java.nio.ByteBuffer;

/**
 * R2004 LZ77 Section Data Decompression
 * Based on OpenDesign Specification v5.4.1 Section 4.7
 */
public class R2004Lz77Decompressor {

    private static class EOFException extends RuntimeException {}

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        ByteBufferBitInput src = new ByteBufferBitInput(ByteBuffer.wrap(compressed));

        int outPos = 0;

        try {
            // Decompression loop (handle any opcode uniformly)
            while (outPos < expectedSize) {
                if (src.position() >= compressed.length * 8) {
                    System.out.printf("[DEBUG] R2004Lz77: EOF at outPos=%d (expected %d)\n", outPos, expectedSize);
                    break;
                }

                int opcode = readByte(src);
                System.out.printf("[DEBUG] R2004Lz77: Opcode=0x%02X at position %d, outPos=%d\n", opcode, src.position() / 8, outPos);

                if (opcode == 0x11) {
                    System.out.printf("[DEBUG] R2004Lz77: Terminator at outPos=%d\n", outPos);
                    break;
                }

                if ((opcode & 0xF0) == 0) {
                    // Literal length opcode
                    int litLength = readLiteralLength(src, opcode);
                    System.out.printf("[DEBUG] R2004Lz77: Literal length=%d\n", litLength);
                    outPos += copyLiteralBytes(src, litLength, output, outPos, compressed.length);
                } else if (opcode < 0x10) {
                    // Invalid range 0x01-0x0F
                    System.out.printf("[WARN] R2004Lz77: Invalid opcode 0x%02X in range 0x01-0x0F\n", opcode);
                    break;
                } else if (opcode == 0x10) {
                    // Long compression offset
                    int compBytes = readLongCompressionOffset(src) + 9;
                    int[] offsetRef = new int[1];
                    int litCount = readTwoByteOffset(src, 0x3FFF, offsetRef);
                    int compOffset = offsetRef[0];
                    System.out.printf("[DEBUG] R2004Lz77: Mode 0x10: bytes=%d offset=%d litCount=%d\n", compBytes, compOffset, litCount);
                    outPos += copyCompressedBytes(output, outPos, compBytes, compOffset);
                    outPos += copyLiteralBytes(src, litCount, output, outPos, compressed.length);
                } else if (opcode >= 0x12 && opcode <= 0x1F) {
                    // Two-byte offset method (0x12-0x1F)
                    int compBytes = (opcode & 0x0F) + 2;
                    int[] offsetRef = new int[1];
                    int litCount = readTwoByteOffset(src, 0x3FFF, offsetRef);
                    int compOffset = offsetRef[0];
                    System.out.printf("[DEBUG] R2004Lz77: Mode 0x12-0x1F: bytes=%d offset=%d litCount=%d\n", compBytes, compOffset, litCount);
                    outPos += copyCompressedBytes(output, outPos, compBytes, compOffset);
                    outPos += copyLiteralBytes(src, litCount, output, outPos, compressed.length);
                } else if (opcode == 0x20) {
                    // Long compression offset + two-byte offset
                    int compBytes = readLongCompressionOffset(src) + 0x21;
                    int[] offsetRef = new int[1];
                    int litCount = readTwoByteOffset(src, 0, offsetRef);
                    int compOffset = offsetRef[0];
                    System.out.printf("[DEBUG] R2004Lz77: Mode 0x20: bytes=%d offset=%d litCount=%d\n", compBytes, compOffset, litCount);
                    outPos += copyCompressedBytes(output, outPos, compBytes, compOffset);
                    outPos += copyLiteralBytes(src, litCount, output, outPos, compressed.length);
                } else if (opcode >= 0x21 && opcode <= 0x3F) {
                    // Two-byte offset method (0x21-0x3F)
                    int compBytes = opcode - 0x1E;
                    int[] offsetRef = new int[1];
                    int litCount = readTwoByteOffset(src, 0, offsetRef);
                    int compOffset = offsetRef[0];
                    System.out.printf("[DEBUG] R2004Lz77: Mode 0x21-0x3F: bytes=%d offset=%d litCount=%d\n", compBytes, compOffset, litCount);
                    outPos += copyCompressedBytes(output, outPos, compBytes, compOffset);
                    outPos += copyLiteralBytes(src, litCount, output, outPos, compressed.length);
                } else if (opcode >= 0x40) {
                    // Single byte offset method (0x40-0xFF)
                    int compBytes = ((opcode >> 4) & 0x0F) - 1;
                    int opcode2 = readByte(src);
                    int compOffset = (int)(((opcode2 << 2) & 0xFFFFFFFFL) | ((opcode & 0x0C) >> 2));
                    compOffset += 1; // Add 1 to offset
                    int litCount = opcode & 0x03;
                    System.out.printf("[DEBUG] R2004Lz77: Mode 0x40+: bytes=%d offset=%d litCount=%d\n", compBytes, compOffset, litCount);
                    outPos += copyCompressedBytes(output, outPos, compBytes, compOffset);
                    outPos += copyLiteralBytes(src, litCount, output, outPos, compressed.length);
                }
            }
        } catch (EOFException e) {
            System.out.printf("[DEBUG] R2004Lz77: EOF during decompression at outPos=%d (expected %d)\n", outPos, expectedSize);
        }

        // Return only the valid decompressed portion
        byte[] result = new byte[outPos];
        System.arraycopy(output, 0, result, 0, outPos);
        return result;
    }

    /**
     * Read a single byte from input, throwing EOFException on EOF
     */
    private int readByte(BitInput input) throws EOFException {
        try {
            return input.readRawChar() & 0xFF;
        } catch (Exception e) {
            throw new EOFException();
        }
    }

    /**
     * Read literal length from opcode (spec section 4.7)
     * 0x01-0x0E: value + 3
     * 0x00: read bytes until non-zero
     */
    private int readLiteralLength(BitInput input, int opcode) throws EOFException {
        int lowbits = opcode & 0x0F;

        if (lowbits == 0) {
            // Read bytes until we get a non-zero one
            int total = 0x0F;
            int nextByte;
            while ((nextByte = readByte(input)) == 0) {
                total += 0xFF;
            }
            total += nextByte;
            return total + 3;
        } else if (lowbits == 0x0F) {
            // 0xF0 - high nibble set, this is actually an opcode
            return 0; // No literal length, the opcode is ready to process
        } else {
            // 0x01-0x0E
            return lowbits + 3;
        }
    }

    /**
     * Read long compression offset (spec section 4.7)
     * 0x01-0xFF: value as-is
     * 0x00: accumulate 0xFF values until non-zero byte
     */
    private int readLongCompressionOffset(BitInput input) throws EOFException {
        int firstByte = readByte(input);

        if (firstByte == 0) {
            // Accumulate 0xFF values
            int total = 0xFF;
            int nextByte;
            while ((nextByte = readByte(input)) == 0) {
                total += 0xFF;
            }
            total += nextByte;
            return total;
        } else {
            return firstByte;
        }
    }

    /**
     * Read two-byte offset (spec section 4.7)
     * Returns litCount, sets offset in offsetRef array
     */
    private int readTwoByteOffset(BitInput input, int plus, int[] offsetRef) throws EOFException {
        int firstByte = readByte(input);
        int secondByte = readByte(input);

        // offset = (firstByte >> 2) | (secondByte << 6)
        int offset = ((firstByte >> 2) & 0xFF) | ((secondByte & 0xFF) << 6);
        offsetRef[0] = offset + plus;

        // litCount = firstByte & 0x03
        return firstByte & 0x03;
    }

    /**
     * Copy compressed (referenced) bytes from earlier in the output
     */
    private int copyCompressedBytes(byte[] output, int outPos, int count, int offset) {
        if (offset <= 0 || offset > outPos) {
            System.out.printf("[WARN] R2004Lz77: Invalid offset %d at outPos %d\n", offset, outPos);
            return 0;
        }

        int refPos = outPos - offset;
        int copied = 0;

        for (int i = 0; i < count && outPos + i < output.length; i++) {
            output[outPos + i] = output[refPos + i];
            copied++;
        }

        return copied;
    }

    /**
     * Copy literal bytes from input stream
     */
    private int copyLiteralBytes(BitInput input, int count, byte[] output, int outPos, int compressedSize) throws EOFException {
        int copied = 0;

        for (int i = 0; i < count && outPos + i < output.length; i++) {
            try {
                output[outPos + i] = (byte) readByte(input);
                copied++;
            } catch (EOFException e) {
                // EOF reached
                System.out.printf("[DEBUG] R2004Lz77: EOF while copying literal bytes, copied %d of %d\n", copied, count);
                throw e; // Re-throw to let the main loop know
            }
        }

        return copied;
    }
}
