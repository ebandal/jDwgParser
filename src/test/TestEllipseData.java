package test;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import java.util.logging.Logger;

public class TestEllipseData {
    private static final Logger log = Logger.getLogger(TestEllipseData.class.getName());

    public static void main(String[] args) throws Exception {
        // Real Ellipse data from Arc.dwg: 20 bytes
        byte[] ellipseData = {
            0x00, (byte)0x80, 0x33, 0x00, 0x08, 0x00, 0x0f, 0x29,
            0x2a, 0x2b, 0x29, (byte)0xa8, 0x39, 0x37, (byte)0xb8, 0x32,
            (byte)0xb9, 0x3a, 0x3c, (byte)0xa7
        };

        System.out.println("=== Testing Ellipse Data (20 bytes) ===");
        System.out.print("Hex: ");
        for (byte b : ellipseData) {
            System.out.printf("%02x ", b & 0xFF);
        }
        System.out.println("\n");

        try {
            System.out.println("Buffer size: " + ellipseData.length + " bytes\n");

            ByteBufferBitInput bitInput = new ByteBufferBitInput(ellipseData);
            BitStreamReader reader = new BitStreamReader(bitInput, DwgVersion.R2004);

            System.out.println("Reading center (3 BD)...");
            System.out.println("Before reading: position=" + bitInput.position());
            double[] center = reader.read3BitDouble();
            System.out.printf("Center: (%.2f, %.2f, %.2f)\n", center[0], center[1], center[2]);

            System.out.println("Reading major axis (3 BD)...");
            double[] major = reader.read3BitDouble();
            System.out.printf("Major: (%.6f, %.6f, %.6f)\n", major[0], major[1], major[2]);

            System.out.println("Reading extrusion (BE)...");
            double[] extrusion = reader.readBitExtrusion();
            System.out.printf("Extrusion: (%.2f, %.2f, %.2f)\n", extrusion[0], extrusion[1], extrusion[2]);

            System.out.println("Reading axis ratio (BD)...");
            double ratio = reader.readBitDouble();
            System.out.printf("Ratio: %.2f\n", ratio);

            System.out.println("Reading start param (BD)...");
            double start = reader.readBitDouble();
            System.out.printf("Start: %.2f\n", start);

            System.out.println("Reading end param (BD)...");
            double end = reader.readBitDouble();
            System.out.printf("End: %.2f\n", end);

            System.out.println("Success!");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
