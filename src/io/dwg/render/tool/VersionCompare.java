package io.dwg.render.tool;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.Renderer;
import io.dwg.render.model.Drawing;

/**
 * Compares how the same drawing renders when it is saved in different DWG versions.
 * The samples contain the same drawings per version (samples/2000/Arc.dwg, samples/2004/Arc.dwg, ...
 * and samples/example_2000.dwg, samples/example_2004.dwg, ...), and R2000 is the verified version,
 * so a file of another version is right when it renders like its R2000 counterpart.
 *
 * Usage: VersionCompare [-out directory] [samples directory]   (default: samples)
 *
 * With -out, the image of every file that differs is written next to the image of its R2000 counterpart.
 */
public class VersionCompare {
    private static final String REFERENCE = "2000";
    private static final String[] VERSIONS = { "r13", "r14", "2004", "2007", "2010", "2013", "2018" };

    /** Outcome for one pair of files. */
    public static final class Result {
        public String version;
        public String name;
        /** why the pair was not compared, or null */
        public String error;
        public int drawn, referenceDrawn;
        /** pixels that differ; -1 if the sizes differ or only one side drew something */
        public long differentPixels;
        public long pixels;
        public BufferedImage image, referenceImage;
        /** the first values that differ between the two drawings */
        public final List<String> differences = new ArrayList<>();

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
        File samples = new File(args.length > first ? args[first] : "samples");

        for (String version : VERSIONS) {
            List<Result> results = compareVersion(samples, version);
            int identical = 0;
            for (Result r : results) {
                if (r.identical()) {
                    identical++;
                }
            }
            System.out.printf("== %s : %d of %d render like R2000%n", version, identical, results.size());
            for (Result r : results) {
                if (r.identical()) {
                    continue;
                }
                System.out.printf("   %-22s %s%n", r.name, r.error != null ? r.error
                        : r.differentPixels < 0 ? "DIFFERENT size or one side empty (drawn " + r.drawn + ", R2000 " + r.referenceDrawn + ")"
                        : "DIFFERENT : " + r.differentPixels + " of " + r.pixels + " pixels (drawn " + r.drawn
                                + ", R2000 " + r.referenceDrawn + ")");
                for (String d : r.differences) {
                    System.out.println("      " + d);
                }
                if (outDir != null && r.error == null && (outDir.isDirectory() || outDir.mkdirs())) {
                    String base = version + "_" + r.name.substring(0, r.name.length() - 4);
                    if (r.image != null) {
                        ImageIO.write(r.image, "png", new File(outDir, base + ".png"));
                    }
                    if (r.referenceImage != null) {
                        ImageIO.write(r.referenceImage, "png", new File(outDir, base + "_r2000.png"));
                    }
                }
            }
        }
    }

    /** Every file of a version that has an R2000 counterpart, compared with it. */
    public static List<Result> compareVersion(File samples, String version) {
        List<Result> results = new ArrayList<>();
        // the same drawing in the version folders
        File dir = new File(samples, version);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".dwg"));
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                File reference = new File(new File(samples, REFERENCE), f.getName());
                if (reference.isFile()) {
                    results.add(compare(version, f, reference));
                }
            }
        }
        // example_<version>.dwg and sample_<version>.dwg next to the folders
        for (String prefix : new String[] { "example_", "sample_" }) {
            File f = new File(samples, prefix + version + ".dwg");
            File reference = new File(samples, prefix + REFERENCE + ".dwg");
            if (f.isFile() && reference.isFile()) {
                results.add(compare(version, f, reference));
            }
        }
        return results;
    }

    public static Result compare(String version, File f, File reference) {
        Result r = new Result();
        r.version = version;
        r.name = f.getName();
        Renderer a = new Renderer();
        Renderer b = new Renderer();
        Drawing drawing;
        Drawing referenceDrawing;
        try {
            drawing = read(f);
            r.image = a.render(drawing);
        } catch (Throwable t) {
            r.error = "DwgReader failed (" + t + ")";
            return r;
        }
        try {
            referenceDrawing = read(reference);
            r.referenceImage = b.render(referenceDrawing);
        } catch (Throwable t) {
            r.error = "DwgReader failed on the R2000 file (" + t + ")";
            return r;
        }
        ModelDiff.diff(drawing, referenceDrawing, r.differences, 6);
        r.drawn = a.drawn;
        r.referenceDrawn = b.drawn;
        if (r.image == null && r.referenceImage == null) {
            r.differentPixels = 0;
        } else if (r.image == null || r.referenceImage == null
                || r.image.getWidth() != r.referenceImage.getWidth()
                || r.image.getHeight() != r.referenceImage.getHeight()) {
            r.differentPixels = -1;
        } else {
            r.pixels = (long) r.image.getWidth() * r.image.getHeight();
            for (int y = 0; y < r.image.getHeight(); y++) {
                for (int x = 0; x < r.image.getWidth(); x++) {
                    if (r.image.getRGB(x, y) != r.referenceImage.getRGB(x, y)) {
                        r.differentPixels++;
                    }
                }
            }
        }
        return r;
    }

    // DwgReader prints diagnostics while it reads; they are not wanted here
    private static Drawing read(File f) throws Exception {
        PrintStream out = System.out;
        PrintStream err = System.err;
        PrintStream mute = new PrintStream(new ByteArrayOutputStream());
        System.setOut(mute);
        System.setErr(mute);
        try {
            DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
            return DocumentDrawing.from(doc);
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }
}
