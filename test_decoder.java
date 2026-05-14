import java.io.File;
import structure.Dwg;

public class test_decoder {
    public static void main(String[] args) throws Exception {
        File file = new File("samples/example_2004.dwg");
        System.out.printf("Testing with DecoderR2004: %s (%,d bytes)\n", file, file.length());

        Dwg dwg = new Dwg();

        try {
            dwg.decode(file);

            System.out.println("\n=== Section Pages ===");
            if (dwg.systemSectionPageList != null) {
                System.out.println("System section pages: " + dwg.systemSectionPageList.size());
                for (structure.sectionpage.SystemSectionPage p : dwg.systemSectionPageList) {
                    System.out.println("  type=0x" + Integer.toHexString(p.header.type)
                            + " comp=" + p.header.compressedSize
                            + " decomp=" + p.header.decompressedSize);
                }
            }
            if (dwg.dataSectionPageList != null) {
                System.out.println("Data section pages: " + dwg.dataSectionPageList.size());
                for (structure.sectionpage.DataSectionPage p : dwg.dataSectionPageList) {
                    System.out.println("  section=" + p.header.sectionNumber
                            + " comp=" + p.header.compressDataSize
                            + " decomp=" + p.header.decompressedPageSize);
                }
            }

            System.out.println("\n=== Parsed Objects ===");
            if (dwg.parsedObjects != null) {
                System.out.println("Total objects: " + dwg.parsedObjects.size());
            } else {
                System.out.println("No objects parsed");
            }
        } catch (Exception e) {
            System.out.printf("Decode error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
