package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.format.common.SectionDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * §4 Section Map 페이지 파싱. 섹션 이름과 페이지 목록 구성.
 */
public class R2004SectionMap {
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2004SectionMap() {}

    public static R2004SectionMap read(BitInput input, long sectionMapByteOffset) throws Exception {
        R2004SectionMap map = new R2004SectionMap();

        System.out.printf("[DEBUG] R2004SectionMap.read: sectionMapByteOffset=0x%X\n", sectionMapByteOffset);

        // sectionMapByteOffset is in bytes, convert to bits
        long actualOffset = sectionMapByteOffset * 8;
        input.seek(actualOffset);

        // 섹션 맵 데이터를 직접 읽기 (압축되지 않았을 수 있음)
        byte[] rawData = new byte[0x10000];
        int readBytes = 0;
        while (readBytes < rawData.length && !input.isEof()) {
            try {
                rawData[readBytes++] = (byte) input.readRawChar();
            } catch (Exception e) {
                break;
            }
        }

        System.out.printf("[DEBUG] R2004SectionMap: Read %d bytes of raw data\n", readBytes);

        // DEBUG: 처음 64바이트 출력
        System.out.printf("[DEBUG] R2004SectionMap: First 64 bytes of raw data:\n");
        for (int i = 0; i < Math.min(64, readBytes); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < readBytes; j++) {
                System.out.printf("%02X ", rawData[i + j] & 0xFF);
            }
            System.out.println();
        }

        if (readBytes < 4) {
            System.out.printf("[WARN] R2004SectionMap: read data too small (%d bytes)\n", readBytes);
            return map;
        }

        // 섹션 수 읽기 (RL = 4 bytes, little-endian)
        int sectionCount = (rawData[0] & 0xFF) |
                           ((rawData[1] & 0xFF) << 8) |
                           ((rawData[2] & 0xFF) << 16) |
                           ((rawData[3] & 0xFF) << 24);
        System.out.printf("[DEBUG] R2004SectionMap: sectionCount from offset 0=%d (0x%X)\n", sectionCount, sectionCount);

        // Sanity check
        if (sectionCount <= 0 || sectionCount > 100) {
            System.out.printf("[WARN] R2004SectionMap: unreasonable sectionCount %d, aborting\n", sectionCount);
            return map;
        }

        // 각 섹션 디스크립터 읽기 (rawData에서)
        // Note: R2004DataSectionDescriptor.read()는 BitInput을 사용하므로 새로운 BitInput 생성 필요
        int descriptorStartOffset = 4;
        io.dwg.core.io.ByteBufferBitInput buf =
            new io.dwg.core.io.ByteBufferBitInput(java.nio.ByteBuffer.wrap(rawData, descriptorStartOffset, readBytes - descriptorStartOffset));

        for (int i = 0; i < sectionCount; i++) {
            try {
                SectionDescriptor desc = R2004DataSectionDescriptor.read(buf);
                System.out.printf("[DEBUG] R2004SectionMap: [%d] name='%s'\n", i, desc.name());
                map.descriptors.add(desc);
            } catch (Exception e) {
                System.out.printf("[WARN] R2004SectionMap: Failed to read descriptor [%d]: %s\n", i, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] R2004SectionMap: Total descriptors loaded: %d\n", map.descriptors.size());
        return map;
    }

    public List<SectionDescriptor> descriptors() {
        return descriptors;
    }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }
}
