package io.dwg.render.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

import io.dwg.render.r2000.DrawingReaderR2000;
import io.dwg.render.Renderer;
import io.dwg.render.model.Drawing;

/**
 * Renders the model space of R2000 (AC1015) files to PNG.
 * Usage: RenderTest [file or directory] [output directory]   (default: samples/2000 target/render)
 */
public class RenderTest {

    public static void main(String[] args) throws Exception {
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
            try {
                d = DrawingReaderR2000.read(f);
            } catch (Exception e) {
                System.out.println(f.getName() + " : FAILED (" + e + ")");
                continue;
            }

            Renderer renderer = new Renderer();
            BufferedImage img = renderer.render(d);
            String summary = f.getName()
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
}
