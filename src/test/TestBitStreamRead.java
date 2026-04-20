package test;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;

public class TestBitStreamRead {
    public static void main(String[] args) throws Exception {
        // Test Arc data: 8 bytes (from sample file)
        byte[] arcData = {(byte)0x0c, 0x11, 0x40, 0x00, 0x00, 0x00, 0x40, 0x6a};

        System.out.println("=== Testing Arc Data (8 bytes) ===");
        System.out.print("Hex: ");
        for (byte b : arcData) {
            System.out.printf("%02x ", b);
        }
        System.out.println();

        ByteBufferBitInput bitInput = new ByteBufferBitInput(arcData);
        BitStreamReader reader = new BitStreamReader(bitInput, DwgVersion.R2004);

        try {
            System.out.println("Reading center (3 BD)...");
            double[] center = reader.read3BitDouble();
            System.out.printf("Center: (%.2f, %.2f, %.2f)\n", center[0], center[1], center[2]);

            System.out.println("Reading radius (BD)...");
            double radius = reader.readBitDouble();
            System.out.printf("Radius: %.2f\n", radius);

            System.out.println("Reading thickness (BT)...");
            double thickness = reader.readBitThickness();
            System.out.printf("Thickness: %.2f\n", thickness);

            System.out.println("Reading extrusion (BE)...");
            double[] extrusion = reader.readBitExtrusion();
            System.out.printf("Extrusion: (%.2f, %.2f, %.2f)\n", extrusion[0], extrusion[1], extrusion[2]);

            System.out.println("Success!");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
