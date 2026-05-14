package io.dwg.core.io;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BitOutput 구현체. ByteArrayOutputStream + 비트 누적 버퍼 사용.
 */
public class ByteBufferBitOutput implements BitOutput {
    private ByteArrayOutputStream baos;
    private int pendingByte;
    private int bitsUsedInPending;

    public ByteBufferBitOutput() {
        this.baos = new ByteArrayOutputStream();
        this.pendingByte = 0;
        this.bitsUsedInPending = 0;
    }

    @Override
    public void writeBit(boolean bit) {
        if (bit) {
            pendingByte |= (1 << (7 - bitsUsedInPending));
        }
        bitsUsedInPending++;
        if (bitsUsedInPending == 8) {
            flush();
        }
    }

    @Override
    public void writeBits(int value, int n) {
        if (n < 0 || n > 32) {
            throw new IllegalArgumentException("n must be between 0 and 32");
        }
        for (int i = n - 1; i >= 0; i--) {
            writeBit(((value >> i) & 1) != 0);
        }
    }

    @Override
    public void writeRawChar(int v) {
        if (bitsUsedInPending > 0) {
            flush();
        }
        baos.write(v & 0xFF);
    }

    @Override
    public void writeRawShort(short v) {
        if (bitsUsedInPending > 0) {
            flush();
        }
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(v);
        baos.write(bb.array(), 0, 2);
    }

    @Override
    public void writeRawLong(int v) {
        if (bitsUsedInPending > 0) {
            flush();
        }
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(v);
        baos.write(bb.array(), 0, 4);
    }

    @Override
    public void writeRawDouble(double v) {
        if (bitsUsedInPending > 0) {
            flush();
        }
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(v);
        baos.write(bb.array(), 0, 8);
    }

    @Override
    public byte[] toByteArray() {
        flush();
        return baos.toByteArray();
    }

    @Override
    public long position() {
        return baos.size() * 8L + bitsUsedInPending;
    }

    private void flush() {
        if (bitsUsedInPending > 0) {
            baos.write(pendingByte);
            pendingByte = 0;
            bitsUsedInPending = 0;
        }
    }
}
