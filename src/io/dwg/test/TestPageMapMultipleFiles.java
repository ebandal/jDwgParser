package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test PageMap reading on all available R2007 files
 */
public class TestPageMapMultipleFiles {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: PageMap Parsing on All R2007 Files");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Find all 2007 sample files
        java.nio.file.Path samplesDir = Paths.get("./samples/2007");
        if (!Files.exists(samplesDir)) {
            System.out.println("Sample directory not found!");
            return;
        }

        java.util.List<java.nio.file.Path> files = new java.util.ArrayList<>();
        try (var stream = Files.list(samplesDir)) {
            stream.filter(p -> p.toString().endsWith(".dwg"))
                  .forEach(files::add);
        }

        System.out.printf("Found %d R2007 files\n\n", files.size());

        int successCount = 0;
        int failCount = 0;

        for (java.nio.file.Path filePath : files) {
            try {
                String filename = filePath.getFileName().toString();
                byte[] fileData = Files.readAllBytes(filePath);
                ByteBuffer buf = ByteBuffer.wrap(fileData);
                BitInput input = new ByteBufferBitInput(buf);

                R2007FileHeader header = R2007FileHeader.read(input);

                long sizeComp = header.pageMapSizeComp();
                long sizeUncomp = header.pageMapSizeUncomp();
                long repeatCount = header.pageMapCorrection();
                long pageMapFileOffset = 0x480L + header.pageMapOffset();

                byte[] decompressed = R2007SystemPageReader.readSystemPage(
                    input, pageMapFileOffset, sizeComp, sizeUncomp, repeatCount
                );

                List<R2007PageMapParser.PageMapEntry> pages =
                    R2007PageMapParser.parsePageMap(decompressed);

                System.out.printf("✅ %s: %d pages (sizes: 0x%X - 0x%X)\n",
                    filename, pages.size(),
                    pages.size() > 0 ? pages.get(0).size : 0,
                    pages.size() > 0 ? pages.get(pages.size()-1).size : 0);

                successCount++;
            } catch (Exception e) {
                System.out.printf("❌ %s: %s\n", filePath.getFileName(), e.getMessage());
                failCount++;
            }
        }

        System.out.printf("\n\nResults: %d success, %d failed\n", successCount, failCount);
    }
}
