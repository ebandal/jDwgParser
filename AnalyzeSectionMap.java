public class AnalyzeSectionMap {
    public static void main(String[] args) {
        String hexData = "01 00 00 00 A0 00 00 00 02 00 00 00 20 7C 00 00 " +
            "03 00 00 00 20 03 00 00 04 00 00 00 A0 05 00 00 " +
            "05 00 00 00 C0 00 00 00 06 00 00 00 80 39 00 00 " +
            "07 00 00 00 20 15 00 00 08 00 00 00 00 02 00 00 " +
            "09 00 00 00 00 36 00 00 0A 00 00 00 20 17 00 00 " +
            "0B 00 00 00 E0 07 00 00 0C 00 00 00 20 0B 00 00 " +
            "0D 00 00 00 A0 0F 00 00 0E 00 00 00 60 40 00 00 " +
            "0F 00 00 00 A0 74 00 00 10 00 00 00 60 72 00 00 " +
            "11 00 00 00 60 4B 00 00 12 00 00 00 00 07 00 00 " +
            "13 00 00 00 E0 00 00 00 14 00 00 00 C0 00 00 00 " +
            "15 00 00 00 80 08 00 00 16 00 00 00 80 0A 00 00 " +
            "17 00 00 00 20 01 00 00 18 00 00 00 00 03 00 00 " +
            "1B 00 00 00 80 02 00 00 1C 00 00 00 80 06 00 00";
        
        String[] parts = hexData.split(" ");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        
        System.out.println("Total bytes: " + bytes.length);
        System.out.println();
        System.out.println("Interpretation: Section ID + Offset pairs (LE32)");
        System.out.println("==================================================");
        
        java.util.Map<Integer, String> sections = new java.util.HashMap<>();
        sections.put(1, "AcDb:Header");
        sections.put(2, "AcDb:AuxHeader");
        sections.put(3, "AcDb:Classes");
        sections.put(4, "AcDb:Handles");
        sections.put(5, "AcDb:Template");
        sections.put(6, "AcDb:ObjFreeSpace");
        sections.put(8, "AcDb:RevHistory");
        sections.put(9, "AcDb:Security");
        sections.put(10, "AcDb:SummaryInfo");
        sections.put(11, "AcDb:VBAProject");
        sections.put(13, "AcDb:Objects");
        sections.put(14, "AcDb:SecdInfo");
        sections.put(21, "AcDb:AppInfo");
        sections.put(28, "AcDb:AppInfoHistory");
        sections.put(27, "AcDb:Preview");
        
        for (int i = 0; i < bytes.length; i += 8) {
            if (i + 8 <= bytes.length) {
                int sid = readLE32(bytes, i);
                int offset = readLE32(bytes, i + 4);
                String name = sections.getOrDefault(sid, "Unknown(" + sid + ")");
                System.out.printf("Section %2d %-20s : offset=0x%06X (%5d)\n", 
                    sid, name, offset, offset);
            }
        }
    }
    
    static int readLE32(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
               ((data[offset+1] & 0xFF) << 8) |
               ((data[offset+2] & 0xFF) << 16) |
               ((data[offset+3] & 0xFF) << 24);
    }
}
