package io.dwg.core.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BitInput 구현체. java.nio.ByteBuffer를 내부 버퍼로 사용.
 */
public class ByteBufferBitInput implements BitInput {
    private ByteBuffer buffer;
    private long bitOffset;
    private int currentByte;
    private int bitsRemainingInByte;

    public ByteBufferBitInput(ByteBuffer buf) {
        this.buffer = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        this.buffer.position(0);
        this.bitOffset = 0;
        this.bitsRemainingInByte = 0;
        this.currentByte = 0;
    }

    public ByteBufferBitInput(byte[] bytes) {
        this(ByteBuffer.wrap(bytes));
    }

    @Override
    public boolean readBit() {
        if (bitsRemainingInByte == 0) {
            loadNextByte();
        }
        bitsRemainingInByte--;
        boolean bit = ((currentByte >> (7 - (8 - 1 - bitsRemainingInByte))) & 1) != 0;
        bitOffset++;
        return bit;
    }

    @Override
    public int readBits(int n) {
        if (n < 0 || n > 32) {
            throw new IllegalArgumentException("n must be between 0 and 32");
        }
        if (n == 0) {
            return 0;
        }

        int result = 0;
        for (int i = 0; i < n; i++) {
            result = (result << 1) | (readBit() ? 1 : 0);
        }
        return result;
    }

    @Override
    public int readRawChar() {
        alignToByteOffset();
        int value = buffer.get() & 0xFF;
        bitOffset += 8;
        return value;
    }

    @Override
    public short readRawShort() {
        alignToByteOffset();
        short value = buffer.getShort();
        bitOffset += 16;
        return value;
    }

    @Override
    public int readRawLong() {
        alignToByteOffset();
        int value = buffer.getInt();
        bitOffset += 32;
        return value;
    }

    @Override
    public double readRawDouble() {
        alignToByteOffset();
        double value = buffer.getDouble();
        bitOffset += 64;
        return value;
    }

    @Override
    public long position() {
        return bitOffset;
    }

    @Override
    public void seek(long bitPos) {
        long bytePos = bitPos / 8;
        int bitInByte = (int)(bitPos % 8);
        buffer.position((int)bytePos);
        bitOffset = bitPos;
        bitsRemainingInByte = 0;
        if (bitInByte > 0) {
            loadNextByte();
            bitsRemainingInByte = 8 - bitInByte;
        }
    }

    @Override
    public boolean isEof() {
        return buffer.position() >= buffer.limit();
    }

    /**
     * 서브 스트림 생성 (섹션 분리에 사용)
     */
    public ByteBufferBitInput slice(long startBit, long lengthBits) {
        long startByte = startBit / 8;
        long lengthBytes = (lengthBits + 7) / 8;
        
        int oldPos = buffer.position();
        buffer.position((int)startByte);
        ByteBuffer slicedBuf = buffer.slice();
        if (lengthBytes < slicedBuf.capacity()) {
            slicedBuf.limit((int)lengthBytes);
        }
        slicedBuf.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(oldPos);
        
        return new ByteBufferBitInput(slicedBuf);
    }

    private void loadNextByte() {
        if (buffer.hasRemaining()) {
            currentByte = buffer.get() & 0xFF;
            bitsRemainingInByte = 8;
        } else {
            currentByte = 0;
            bitsRemainingInByte = 0;
        }
    }

    private void alignToByteOffset() {
        if (bitsRemainingInByte > 0 && bitsRemainingInByte < 8) {
            // We're in the middle of a byte, skip to next byte
            bitOffset += bitsRemainingInByte;
            bitsRemainingInByte = 0;
            currentByte = 0;
            // buffer.position() already moved forward by loadNextByte(), no need to adjust
        }
    }
}
