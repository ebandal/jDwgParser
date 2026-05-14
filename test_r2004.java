import io.dwg.core.io.*;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.r2004.R2004FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class test_r2004 {
    public static void main(String[] args) throws Exception {
        String filepath = "../samples/2004/Arc.dwg";
        byte[] fileBytes = Files.readAllBytes(Paths.get(filepath));
        System.out.println("File size: " + fileBytes.length + " bytes");
        System.out.println("First 6 bytes: " + new String(fileBytes, 0, 6));
        
        ByteBufferBitInput input = new ByteBufferBitInput(fileBytes);
        R2004FileStructureHandler handler = new R2004FileStructureHandler();
        
        System.out.println("\n=== Reading Header ===");
        FileHeaderFields header = handler.readHeader(input);
        System.out.println("Header parsed successfully");
        System.out.println("Security flags: 0x" + Integer.toHexString(header.securityFlags()));
        
        System.out.println("\n=== Reading Sections ===");
        Map<String, SectionInputStream> sections = handler.readSections(input, header);
        System.out.println("Sections read: " + sections.keySet());
    }
}
