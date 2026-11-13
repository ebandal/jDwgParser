package io.dwg.render.tool;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
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
 * Renders R2000 (AC1015) files twice, once from {@link DwgReader} and once from the reader of
 * io.dwg.render.r2000, and counts the pixels that differ. The two images should be identical.
 *
 * Usage: RenderCompare [file or directory]...   (default: samples/2000)
 *        RenderCompare -out directory [file or directory]...   also writes both images of files that differ
 */
public class RenderCompare {

    /** Outcome for one file. */
    public static final class Result {
        public String file;
        /** why the file was not compared, or null */
        public String error;
        public int width, height;
        public int referenceWidth, referenceHeight;
        /** pixels that differ; -1 if the sizes differ or only one side drew something */
        public long differentPixels;
        public int drawn, referenceDrawn;
        public BufferedImage image, referenceImage;

        public boolean identical() {
            return error == null && differentPixels == 0;
        }
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("").setLevel(Level.OFF);
        File outDir = null;
        int first = 0;
        if (args.length >= 2 && args[0].equals("-out")) {
            outDir = new File(args[1]);
            first = 2;
        }
        String[] paths = new String[Math.max(args.length - first, 0)];
        System.arraycopy(args, first, paths, 0, paths.length);
        List<File> files = ReaderCheck.collect(paths.length > 0 ? paths : new String[] { "samples/2000" });

        int identical = 0;
        for (File f : files) {
            Result r = compare(f);
            if (r.identical()) {
                identical++;
            }
            if (r.error != null) {
                System.out.printf("%-34s %s%n", r.file, r.error);
            } else {
                System.out.printf("%-34s drawn=%d (reference %d)  %s%n", r.file, r.drawn, r.referenceDrawn,
                        r.differentPixels == 0 ? (r.image == null ? "nothing to draw on either side" : "identical")
                        : r.differentPixels < 0 ? "DIFFERENT : " + size(r.width, r.height) + " vs reference "
                                + size(r.referenceWidth, r.referenceHeight)
                        : "DIFFERENT : " + r.differentPixels + " of " + (long) r.width * r.height + " pixels");
            }
            if (outDir != null && !r.identical() && r.error == null && (outDir.isDirectory() || outDir.mkdirs())) {
                String name = f.getName().substring(0, f.getName().length() - 4);
                if (r.image != null) {
                    ImageIO.write(r.image, "png", new File(outDir, name + "_reader.png"));
                }
                if (r.referenceImage != null) {
                    ImageIO.write(r.referenceImage, "png", new File(outDir, name + "_reference.png"));
                }
            }
        }
        System.out.printf("%nfiles=%d, identical=%d%n", files.size(), identical);
    }

    private static String size(int w, int h) {
        return w == 0 ? "no image" : w + "x" + h;
    }

    /** Renders one file both ways and compares the images. */
    public static Result compare(File f) {
        Result r = new Result();
        r.file = f.getName();

        Drawing reference;
        try {
            reference = DrawingReaderR2000.read(f);
        } catch (Exception e) {
            r.error = "skipped, not readable as R2000 (" + e + ")";
            return r;
        }
        Drawing drawing;
        PrintStream out = System.out;
        PrintStream err = System.err;
        PrintStream mute = new PrintStream(new ByteArrayOutputStream());
        System.setOut(mute);
        System.setErr(mute);
        try {
            DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
            drawing = DocumentDrawing.from(doc);
        } catch (Throwable t) {
            r.error = "DwgReader failed (" + t + ")";
            return r;
        } finally {
            System.setOut(out);
            System.setErr(err);
        }

        Renderer a = new Renderer();
        Renderer b = new Renderer();
        r.image = a.render(drawing);
        r.referenceImage = b.render(reference);
        r.drawn = a.drawn;
        r.referenceDrawn = b.drawn;
        if (r.image != null) {
            r.width = r.image.getWidth();
            r.height = r.image.getHeight();
        }
        if (r.referenceImage != null) {
            r.referenceWidth = r.referenceImage.getWidth();
            r.referenceHeight = r.referenceImage.getHeight();
        }
        if (r.image == null && r.referenceImage == null) {
            r.differentPixels = 0;
        } else if (r.width != r.referenceWidth || r.height != r.referenceHeight) {
            r.differentPixels = -1;
        } else {
            long n = 0;
            for (int y = 0; y < r.height; y++) {
                for (int x = 0; x < r.width; x++) {
                    if (r.image.getRGB(x, y) != r.referenceImage.getRGB(x, y)) {
                        n++;
                    }
                }
            }
            r.differentPixels = n;
        }
        return r;
    }
}
