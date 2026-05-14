package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.io.*;
import java.nio.file.*;

/**
 * Test RS decoder with actual R2007 DWG file
 */
public class R2007RealFileTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2007 Real File Test ===\n");

        // Try multiple path variations
        String[] possiblePaths = {
            "/c/workspace_ebandal/jDwgParser/samples/2007/Line.dwg",
            "samples/2007/Line.dwg",
            "../samples/2007/Line.dwg"
        };

        String filePath = null;
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                filePath = path;
                break;
            }
        }

        if (filePath == null) {
            System.out.println("ERROR: Test file not found. Tried:");
            for (String path : possiblePaths) {
                System.out.println("  " + path);
            }
            return;
        }
        File file = new File(filePath);

        System.out.println("Reading file: " + filePath);
        System.out.println("File size: " + file.length() + " bytes\n");

        byte[] data = Files.readAllBytes(file.toPath());

        // R2007 header structure (per libredwg):
        // 0x00-0x05: File ID signature ("AC1021")
        // 0x06-0x39: Live data fields (52 bytes)
        // 0x3A-0x7F: Padding (70 bytes)
        // 0x80-0x3d7: Reed-Solomon encoded header (0x3d8 = 984 bytes)

        System.out.println("File signature: " + new String(data, 0, 6));
        System.out.printf("Version: %02x %02x %02x %02x\n", data[6] & 0xFF, data[7] & 0xFF,
                         data[8] & 0xFF, data[9] & 0xFF);

        // The RS-encoded data starts at offset 0x80 (libredwg decode_r2007.c:1214)
        // and is 0x3d8 = 984 bytes long (3 * 255 + 219)
        System.out.println("\n=== RS-Encoded Data ===");
        if (data.length >= 0x80 + 984) {
            byte[] rsData = new byte[984];
            System.arraycopy(data, 0x80, rsData, 0, 984);

            System.out.printf("RS data offset: 0x6A (%d)\n", 0x6A);
            System.out.printf("RS data length: %d bytes (3 * 255 + 219)\n", 984);
            System.out.printf("First 32 bytes of RS data: ");
            for (int i = 0; i < Math.min(32, rsData.length); i++) {
                System.out.printf("%02x ", rsData[i] & 0xFF);
            }
            System.out.println();

            System.out.println("\n=== Decoding RS Blocks ===");
            System.out.println("(This will decode 3 x 255-byte RS blocks)");

            byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsData);

            if (decoded != null) {
                System.out.println("\n=== Decoding Result ===");
                System.out.printf("Decoded %d bytes successfully\n", decoded.length);
                System.out.printf("First 32 bytes of decoded data: ");
                for (int i = 0; i < Math.min(32, decoded.length); i++) {
                    System.out.printf("%02x ", decoded[i] & 0xFF);
                }
                System.out.println();
                System.out.println("SUCCESS: RS decoder worked on real R2007 file!");
            } else {
                System.out.println("\nFAILED: RS decoder returned null");
            }
        } else {
            System.out.println("ERROR: File too small for RS data extraction");
        }
    }
}
