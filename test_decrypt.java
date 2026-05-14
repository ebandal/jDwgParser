import java.nio.file.*;
public class test_decrypt {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2004/Arc.dwg"));
        
        // Data at offset 0x100 (Header section start)
        byte[] encrypted = new byte[32];
        System.arraycopy(data, 0x100, encrypted, 0, 32);
        
        System.out.println("Encrypted 32 bytes at 0x100:");
        for (int i = 0; i < 32; i += 16) {
            for (int j = 0; j < 16 && i + j < 32; j++) {
                System.out.printf("%02X ", encrypted[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Try decryption with secMask = 0x4164526B (from test output)
        long secMask = 0x4164526BL;
        byte[] decrypted = new byte[32];
        for (int i = 0; i < 32; i++) {
            decrypted[i] = (byte)(encrypted[i] ^ ((secMask >> ((i & 3) * 8)) & 0xFF));
        }
        
        System.out.println("\nDecrypted with secMask=0x4164526B:");
        for (int i = 0; i < 32; i += 16) {
            for (int j = 0; j < 16 && i + j < 32; j++) {
                System.out.printf("%02X ", decrypted[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Try to read as LE32 integers
        System.out.println("\nAs LE32 values:");
        for (int i = 0; i < 32; i += 4) {
            long val = (decrypted[i] & 0xFFL) |
                      ((decrypted[i+1] & 0xFFL) << 8) |
                      ((decrypted[i+2] & 0xFFL) << 16) |
                      ((decrypted[i+3] & 0xFFL) << 24);
            System.out.printf("  0x%02X: 0x%08X (%d)\n", i, val, val);
        }
    }
}
