import java.nio.file.*;
public class analyze_sections {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2004/Arc.dwg"));
        
        // Section 1: Header
        // offset=0x100, uncompressed_size=0xA0 (160)
        // Expected: [32-byte header] + [compressed data]
        
        // Read page 1 header at 0x100
        byte[] header1 = new byte[32];
        System.arraycopy(data, 0x100, header1, 0, 32);
        
        long secMask = 0x4164526BL;
        byte[] decr1 = new byte[32];
        for (int i = 0; i < 32; i++) {
            decr1[i] = (byte)(header1[i] ^ ((secMask >> ((i & 3) * 8)) & 0xFF));
        }
        
        long compSize1 = ((decr1[8] & 0xFF) | ((decr1[9] & 0xFF) << 8) | 
                         ((decr1[10] & 0xFF) << 16) | ((decr1[11] & 0xFF) << 24));
        long decompSize1 = ((decr1[12] & 0xFF) | ((decr1[13] & 0xFF) << 8) | 
                           ((decr1[14] & 0xFF) << 16) | ((decr1[15] & 0xFF) << 24));
        
        System.out.println("Page 1 (Header):");
        System.out.println("  compSize = " + compSize1 + " (0x" + Long.toHexString(compSize1) + ")");
        System.out.println("  decompSize = " + decompSize1 + " (0x" + Long.toHexString(decompSize1) + ")");
        System.out.println("  Page 1 occupies: 32 (header) + " + compSize1 + " (data) = " + (32 + compSize1) + " bytes");
        System.out.println("  Section total: 160 bytes");
        System.out.println("  Remaining for page 2: " + (160 - 32 - compSize1) + " bytes");
        
        // If there's a page 2, it would start at 0x100 + 32 + compSize1
        long page2Offset = 0x100 + 32 + compSize1;
        System.out.println("\nIf page 2 exists, it starts at offset: 0x" + Long.toHexString(page2Offset));
        
        // Check if there's enough data for page 2
        if (page2Offset + 32 <= data.length) {
            byte[] header2 = new byte[32];
            System.arraycopy(data, (int)page2Offset, header2, 0, 32);
            
            long secMask2 = 0x4164536bL ^ page2Offset;
            byte[] decr2 = new byte[32];
            for (int i = 0; i < 32; i++) {
                decr2[i] = (byte)(header2[i] ^ ((secMask2 >> ((i & 3) * 8)) & 0xFF));
            }
            
            long pageType2 = ((decr2[0] & 0xFF) | ((decr2[1] & 0xFF) << 8) | 
                             ((decr2[2] & 0xFF) << 16) | ((decr2[3] & 0xFF) << 24));
            
            System.out.println("Page 2 header found!");
            System.out.println("  pageType = 0x" + Long.toHexString(pageType2));
        } else {
            System.out.println("No page 2 (not enough data)");
        }
    }
}
