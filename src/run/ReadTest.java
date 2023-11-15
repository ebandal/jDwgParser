package run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

import decode.DwgParseException;
import structure.Dwg;

public class ReadTest {
    
    public static void main(String[] args) {
        
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.ALL);
        for (Handler h: rootLogger.getHandlers()) {
            h.setLevel(Level.ALL);
        }
        
        Dwg dwg = new Dwg();
        try {
            dwg.decode(new File("dwg\\2_KANAL_1_FILE_2.dwg"));
        } catch (DwgParseException e) {
            e.printStackTrace();
        }
    }

    public static void classify( ) {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.ALL);
        for (Handler h: rootLogger.getHandlers()) {
            h.setLevel(Level.ALL);
        }
        
        try {
            Stream<Path> streams = Files.walk(Paths.get("DWG"));
            streams.filter(p -> p.toFile().isFile())
                    .forEach(p -> {
                        Dwg dwg = new Dwg();
                        try {
                            dwg.decode(p.toFile());
                            switch(dwg.header.versionId) {
                            case "AC1012":  moveToFolder(p, "AC1012");  break;
                            case "AC1014":  moveToFolder(p, "AC1014");  break;
                            case "AC1015":  moveToFolder(p, "AC1015");  break;
                            case "AC1018":  moveToFolder(p, "AC1018");  break;
                            case "AC1021":  moveToFolder(p, "AC1021");  break;
                            case "AC1024":  moveToFolder(p, "AC1024");  break;
                            case "AC1027":  moveToFolder(p, "AC1027");  break;
                            case "AC1032":  moveToFolder(p, "AC1032");  break;
                            default:        moveToFolder(p, "FAIL");    break;
                            }
                        } catch (DwgParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void moveToFolder(Path p, String folderName) {
        try {
            Files.createDirectories(Paths.get(folderName), null);
            Path target = Paths.get(folderName, p.getFileName().toString());
            Files.move(p,  target,  StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
