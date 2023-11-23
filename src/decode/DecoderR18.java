package decode;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class DecoderR18 {

    public static byte[] decompressR18(byte[] srcBuf, int srcIndex) {
        ByteBuffer bBuffer = ByteBuffer.allocate(srcBuf.length * 2);
        int compressedBytes = 0;
        int compOffset = 0;
        int litCount = 0;
        AtomicInteger srcOffset = new AtomicInteger(srcIndex);
        
        while(srcOffset.get() <= srcBuf.length) {
            byte opCode = srcBuf[srcOffset.getAndIncrement()];
            if (opCode == 0x10) {
                compressedBytes = longCompressedOffset(srcBuf, srcOffset) + 9;
                byte firstByte = srcBuf[srcOffset.getAndIncrement()];
                compOffset = ((firstByte>>2) | (srcBuf[srcOffset.getAndIncrement()]<<6)) + 0x3FFF;
                litCount = (firstByte & 0x03);
                if (litCount == 0) {
                    litCount = literalLength(srcBuf, srcOffset);
                }
            } else if (opCode == 0x11) {
                break;
            } else if (opCode == 0x12&& opCode <= 0x1F) {
                compressedBytes = (opCode & 0xF)+2;
                byte firstByte = srcBuf[srcOffset.getAndIncrement()];
                compOffset = ((firstByte>>2)|(srcBuf[srcOffset.getAndIncrement()]<<6)) + 0x3FFF;
                litCount = (firstByte & 0x03);
                if (litCount == 0) {
                    litCount = literalLength(srcBuf, srcOffset);
                }
            } else if (opCode == 0x20) {
                compressedBytes = longCompressedOffset(srcBuf, srcOffset) + 0x21;
                byte firstByte = srcBuf[srcOffset.getAndIncrement()];
                compOffset = ((firstByte>>2)|(srcBuf[srcOffset.getAndIncrement()]<<6));
                litCount = (firstByte & 0x03);
                if (litCount == 0) {
                    litCount = literalLength(srcBuf, srcOffset);
                }
            } else if (opCode == 0x21 && opCode <= 0x3F) {
                compressedBytes = opCode - 0x1E;
                byte firstByte = srcBuf[srcOffset.getAndIncrement()];
                compOffset = ((firstByte>>2)|(srcBuf[srcOffset.getAndIncrement()]<<6));
                litCount = (firstByte & 0x03);
                if (litCount == 0) {
                    litCount = literalLength(srcBuf, srcOffset);
                }
            } else if (opCode == 0x40 && opCode <= 0xFF) {
                compressedBytes = ((opCode & 0xF0)>>4) -1;
                int opCode2 = srcBuf[srcOffset.getAndIncrement()];
                compOffset = (opCode2<<2)|((opCode & 0x0C)>>2);
                switch(opCode & 0x03) {
                case 0x00:
                    litCount = literalLength(srcBuf, srcOffset);
                    break;
                case 0x01:
                    litCount = 1;
                    break;
                case 0x02:
                    litCount = 2;
                    break;
                case 0x03:
                    litCount = 3;
                    break;
                }
            }
            
            bBuffer.put(srcBuf, srcOffset.get(), litCount);
        }
        return bBuffer.array();
    }
    
    private static int literalLength(byte[] srcBuf, AtomicInteger srcOffset) {
        int ret = 0;
        
        byte litLength = srcBuf[srcOffset.getAndIncrement()];
        if (litLength==0) {
            ret += 0x0F;
            while(srcBuf[srcOffset.getAndIncrement()]==0x00) {
                ret += 0xFF;
            }
            ret += srcBuf[srcOffset.getAndIncrement()];
            ret += 3;
        } else if (litLength>=0x01 && litLength<=0x0E) {
            ret = litLength + 3;
        } else if (litLength==0xF0) {
            ret = 0;
        }
        return ret;
    }
    
    private static int longCompressedOffset(byte[] srcBuf, AtomicInteger srcOffset) {
        int ret = 0;
        
        while(srcBuf[srcOffset.getAndIncrement()]==0x00) {
            ret += 0xFF;
        }
        ret += srcBuf[srcOffset.getAndIncrement()];
        
        return ret;
    }
}
