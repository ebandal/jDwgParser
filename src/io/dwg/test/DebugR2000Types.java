package io.dwg.test;

import io.dwg.api.DwgReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugR2000Types {
    public static void main(String[] args) throws Exception {
        System.out.println("Parsing single R2000 Arc.dwg file to see type codes\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        var doc = DwgReader.defaultReader().open(data);

        System.out.printf("\nTotal objects parsed: %d\n", doc.objectMap().size());
    }
}
