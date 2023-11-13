package run;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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

}
