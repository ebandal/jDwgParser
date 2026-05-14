import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class test_rs_order {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("samples/2007/Arc.dwg"));
        byte[] rsData = new byte[765];
        System.arraycopy(fileData, 0x80, rsData, 0, 765);

        System.out.println("Testing different RS block interpretations:\n");

        // Try 1: Standard order (block 0,1,2)
        System.out.println("Try 1: Standard order (0,1,2)");
        testOrder(rsData, new int[]{0,1,2});

        // Try 2: Reversed order (2,1,0)
        System.out.println("\nTry 2: Reversed order (2,1,0)");
        testOrder(rsData, new int[]{2,1,0});

        // Try 3: Rotated (1,2,0)
        System.out.println("\nTry 3: Rotated (1,2,0)");
        testOrder(rsData, new int[]{1,2,0});

        // Try 4: What if it's 239 bytes data + 16 bytes parity per block?
        System.out.println("\nTry 4: Check byte patterns");
        byte[] combined = new byte[717];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(rsData, i * 255, combined, i * 239, 239);
        }
        System.out.println("  Checking for 'AC' magic or valid headers");
        for (int off = 0; off < 50; off += 8) {
            ByteBuffer bb = ByteBuffer.wrap(combined, off, 8);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            long val = bb.getLong();
            if ((val & 0xFFFF) == 0x4341 || (val & 0xFFFFFF00L) == 0) { // 'AC' or looks like size
                System.out.printf("    @0x%02X: 0x%016X (potential match)\n", off, val);
            }
        }
    }

    static void testOrder(byte[] rsData, int[] order) {
        byte[] combined = new byte[717];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(rsData, order[i] * 255, combined, i * 239, 239);
        }
        ByteBuffer bb = ByteBuffer.wrap(combined);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        long headerSize = bb.getLong(0);
        System.out.printf("  headerSize = 0x%016X\n", headerSize);
    }
}
