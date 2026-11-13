package io.dwg.render.tool;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.Renderer;
import io.dwg.render.model.Drawing;
import io.dwg.render.r2000.DrawingReaderR2000;

/**
 * Renders the model space of DWG files to PNG. The files are read with {@link DwgReader}.
 *
 * Usage: RenderTest [-reference] [file or directory] [output directory]   (default: samples/2000 target/render)
 *
 * -reference reads the files with the R2000-only reader of io.dwg.render.r2000 instead, for comparison.
 */
public class RenderTest {

    public static void main(String[] args) throws Exception {
        Logger.getLogger("").setLevel(Level.OFF);
        boolean reference = args.length > 0 && args[0].equals("-reference");
        if (reference) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        File target = new File(args.length > 0 ? args[0] : "samples/2000");
        File outDir = new File(args.length > 1 ? args[1] : "target/render");
        File[] files = target.isDirectory()
                ? target.listFiles((d, n) -> n.toLowerCase().endsWith(".dwg"))
                : new File[] { target };
        if (files == null || files.length == 0) {
            System.out.println("no .dwg file in " + target);
            return;
        }
        Arrays.sort(files);
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            System.out.println("cannot create " + outDir);
            return;
        }

        for (File f : files) {
            Drawing d;
            String version = "";
            try {
                if (reference) {
                    d = DrawingReaderR2000.read(f);
                } else {
                    DwgDocument doc = open(f);
                    version = " [" + doc.version() + "]";
                    d = DocumentDrawing.from(doc);
                }
            } catch (Throwable e) {
                System.out.println(f.getName() + " : FAILED (" + e + ")");
                continue;
            }

            Renderer renderer = new Renderer();
            BufferedImage img = renderer.render(d);
            String summary = f.getName() + version
                    + " : entities=" + d.entityCount + ", decoded=" + d.decodedCount + ", failed=" + d.failedCount
                    + ", layers=" + d.layers.size() + ", blocks=" + d.blocks.size()
                    + " | drawn=" + renderer.drawn + ", inserts expanded=" + renderer.insertsExpanded
                    + ", hidden=" + renderer.hidden + ", missing blocks=" + renderer.missingBlocks;
            if (img == null) {
                System.out.println(summary + " -> nothing to draw");
                continue;
            }
            String name = f.getName().substring(0, f.getName().length() - 4) + ".png";
            File out = new File(outDir, name);
            ImageIO.write(img, "png", out);
            System.out.println(summary + " -> " + out.getName() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
        }
    }

    // DwgReader prints diagnostics while it reads; they are not wanted here
    private static DwgDocument open(File f) throws Exception {
        PrintStream out = System.out;
        PrintStream err = System.err;
        PrintStream mute = new PrintStream(new ByteArrayOutputStream());
        System.setOut(mute);
        System.setErr(mute);
        try {
            return DwgReader.defaultReader().open(f.toPath());
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }
}
