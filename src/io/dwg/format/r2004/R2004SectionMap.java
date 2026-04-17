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

        // 두 가지 가능성: sectionMapByteOffset이 바이트 단위인지 비트 단위인지 확인
        // 먼저 바이트 단위로 시도
        long actualOffsetBytes = sectionMapByteOffset * 8;
        input.seek(actualOffsetBytes);

        // 처음 32바이트 읽어서 확인
        System.out.printf("[DEBUG] R2004SectionMap: trying byte offset 0x%X (bit offset 0x%X)\n", sectionMapByteOffset, actualOffsetBytes);
        byte[] preview = new byte[32];
        long savedPos = actualOffsetBytes;
        for (int i = 0; i < 32 && i + savedPos < Long.MAX_VALUE; i++) {
            preview[i] = (byte) input.readRawChar();
        }
        System.out.printf("[DEBUG] R2004SectionMap: First 32 bytes at offset: ");
        for (int i = 0; i < 32; i++) {
            System.out.printf("%02X ", preview[i] & 0xFF);
        }
        System.out.println();

        // 다시 처음 위치로 돌아가기
        input.seek(savedPos);

        // 섹션 수 읽기 (RL = 4 bytes)
        int sectionCount = input.readRawLong();
        System.out.printf("[DEBUG] R2004SectionMap: sectionCount=%d (0x%X)\n", sectionCount, sectionCount);

        // Sanity check
        if (sectionCount <= 0 || sectionCount > 100) {
            System.out.printf("[WARN] R2004SectionMap: unreasonable sectionCount %d\n", sectionCount);
            // 대안: offset이 비트 단위라고 가정하고 다시 시도
            System.out.printf("[DEBUG] R2004SectionMap: trying bit offset interpretation: 0x%X bits\n", sectionMapByteOffset);
            input.seek(sectionMapByteOffset);
            sectionCount = input.readRawLong();
            System.out.printf("[DEBUG] R2004SectionMap (bit offset): sectionCount=%d (0x%X)\n", sectionCount, sectionCount);

            if (sectionCount <= 0 || sectionCount > 100) {
                System.out.printf("[WARN] R2004SectionMap: both interpretations failed, aborting\n");
                return map;
            }
        }

        for (int i = 0; i < sectionCount; i++) {
            try {
                SectionDescriptor desc = R2004DataSectionDescriptor.read(input);
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
