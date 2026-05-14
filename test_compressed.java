import java.nio.file.*;
public class test_compressed {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2004/Arc.dwg"));
        
        // Compressed data should be at 0x100 + 0x20 = 0x120
        // and be 128 bytes long (from decrypted header)
        System.out.println("Compressed data at 0x120-0x19F (128 bytes):");
        for (int i = 0x120; i < 0x1A0; i += 16) {
            System.out.printf("  0x%03X: ", i);
            for (int j = 0; j < 16 && i + j < 0x1A0; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Next section (AuxHeader) should start at 0x1A0
        System.out.println("\nNext section encrypted header at 0x1A0:");
        byte[] nextEncrypted = new byte[32];
        System.arraycopy(data, 0x1A0, nextEncrypted, 0, 32);
        for (int i = 0; i < 32; i += 16) {
            System.out.printf("  0x%03X: ", i);
            for (int j = 0; j < 16 && i + j < 32; j++) {
                System.out.printf("%02X ", nextEncrypted[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Try decrypting with secMask for 0x1A0
        long secMask = 0x4164536bL ^ 0x1A0L;
        byte[] nextDecrypted = new byte[32];
        for (int i = 0; i < 32; i++) {
            nextDecrypted[i] = (byte)(nextEncrypted[i] ^ ((secMask >> ((i & 3) * 8)) & 0xFF));
        }
        
        System.out.println("\nAuxHeader decrypted with secMask=0x" + Long.toHexString(secMask) + ":");
        for (int i = 0; i < 32; i += 16) {
            System.out.printf("  0x%03X: ", i);
            for (int j = 0; j < 16 && i + j < 32; j++) {
                System.out.printf("%02X ", nextDecrypted[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
