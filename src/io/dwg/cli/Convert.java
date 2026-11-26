package io.dwg.cli;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import io.dwg.api.DwgReadException;
import io.dwg.api.DwgReader;
import io.dwg.api.Version;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.PngRows;
import io.dwg.render.PngWriter;
import io.dwg.render.Renderer;
import io.dwg.render.model.Drawing;

/**
 * Converts DWG files to PNG, SVG or PDF from the command line, and merges split PNGs:
 * <pre>
 *   java -jar jdwgparser.jar drawing.dwg                    drawing.png next to it
 *   java -jar jdwgparser.jar drawing.dwg out.svg            the format from the extension
 *   java -jar jdwgparser.jar folder/ out/ --format pdf      every .dwg of a folder (and below)
 *   java -jar jdwgparser.jar drawing.dwg --split            PNG tiles and drawing.tiles
 *   java -jar jdwgparser.jar merge drawing.tiles [out.png]  the tiles as one PNG
 * </pre>
 * The size of the picture: {@code --dpi N} fits the drawing on the paper of its model space
 * layout at N dots per inch, {@code --px-per-unit N} gives N pixels per drawing unit,
 * {@code --size N} makes the longer side N pixels. Without one of them: the paper at 300 dpi, or
 * 1 pixel per unit when the drawing has no paper. {@code --scale F} then shrinks (or enlarges)
 * that size by F, given as {@code 1/4}, {@code 1/8}, {@code 0.5} and the like.
 * <p>A PNG is made whole in memory (fast; its memory grows with its area), or with
 * {@code --split} as tiles of {@code --tile N} pixels (default 2048) drawn a band at a time
 * (little memory; slower, as each band goes through the drawing again). {@code merge} joins the
 * tiles into one PNG a row at a time, also in little memory.
 * <p>Each file is reported on one line; the exit status is 0 when all were converted, 1 when
 * some were not, 2 for wrong arguments. {@code --version} prints the version line that a bug
 * report should carry; it is printed too when a file fails. With {@code --json} the standard
 * output is one JSON document instead, for programs (and AI agents) to read.
 */
public final class Convert {

    static final String LICENSE_LINE =
            "Free for noncommercial use; commercial use needs a license (LICENSE, LICENSING.md).";
    static final int DEFAULT_DPI = 300;
    static final int DEFAULT_TILE = 2048;
    /** Memory for the band of a split picture. */
    static final long BAND_BYTES = 32L << 20;

    private Convert() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** The options of one conversion. */
    static final class Options {
        String format;
        int size;
        double pxPerUnit;
        /** the factor applied to the size chosen */
        double scale = 1;
        int dpi;
        boolean white;
        long maxBytes;
        boolean split;
        int tile = DEFAULT_TILE;
        boolean json;
    }

    /** What one conversion made. */
    static final class Result {
        String format;
        int width;
        int height;
        String sizing;
        int tileColumns;
        int tileRows;
        Path tilesFile;
        int entities;
        int decoded;
        int failedToDecode;

        /** The text after "ok" on the result line. */
        String text() {
            if (tilesFile != null) {
                return String.format("%d x %d px in %d x %d tiles, %s, %s", width, height, tileColumns, tileRows,
                        tilesFile.getFileName(), sizing);
            }
            return format.equals("png") ? width + "x" + height + " px, " + sizing : sizing;
        }

        Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("format", format);
            m.put("width", width);
            m.put("height", height);
            m.put("sizing", sizing);
            if (tilesFile != null) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("columns", tileColumns);
                t.put("rows", tileRows);
                t.put("file", tilesFile.toString());
                t.put("names", tileName(tilesFile.getFileName().toString().replaceAll("\\.tiles$", ""), 0, 0)
                        .replace("[1x1]", "[<column>x<row>]"));
                m.put("tiles", t);
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("total", entities);
            e.put("decoded", decoded);
            e.put("failedToDecode", failedToDecode);
            e.put("unsupportedType", Math.max(0, entities - decoded - failedToDecode));
            m.put("entities", e);
            return m;
        }
    }

    /** The start of every JSON document: what made it. */
    private static Map<String, Object> header(int status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tool", "jDwgParser");
        m.put("version", Version.number());
        m.put("status", status);
        return m;
    }

    private static int printJson(Map<String, Object> doc, int status) {
        doc.put("status", status);
        if (status == 1) {
            doc.put("bugReport", Version.describe());
        }
        doc.put("license", LICENSE_LINE);
        System.out.println(Json.write(doc));
        return status;
    }

    /** Runs the command with {@code args} and returns its exit status, without exiting. */
    public static int run(String[] args) {
        boolean json = Arrays.asList(args).contains("--json");
        if (args.length > 0 && args[0].equals("merge")) {
            return reported(merge(args, json));
        }
        if (args.length == (json ? 2 : 1) && (Arrays.asList(args).contains("--version") || Arrays.asList(args).contains("-V"))) {
            if (json) {
                Map<String, Object> doc = header(0);
                doc.put("built", Version.built());
                doc.put("java", System.getProperty("java.version"));
                doc.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
                doc.put("bugReport", Version.describe());
                return printJson(doc, 0);
            }
            System.out.println(Version.describe());
            System.out.println(LICENSE_LINE);
            return 0;
        }
        List<String> files = new ArrayList<>();
        Options o = new Options();
        o.json = json;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--format": o.format = args[++i].toLowerCase(Locale.ROOT); break;
                    case "--size": o.size = Integer.parseInt(args[++i]); break;
                    case "--px-per-unit": o.pxPerUnit = Double.parseDouble(args[++i]); break;
                    case "--scale": o.scale = factor(args[++i]); break;
                    case "--dpi": o.dpi = Integer.parseInt(args[++i]); break;
                    case "--background": o.white = args[++i].equalsIgnoreCase("white"); break;
                    case "--max-mb": o.maxBytes = Long.parseLong(args[++i]) << 20; break;
                    case "--split": o.split = true; break;
                    case "--tile": o.tile = Integer.parseInt(args[++i]); break;
                    case "--json": break;
                    case "-h":
                    case "--help":
                        usage();
                        return 0;
                    default:
                        if (args[i].startsWith("--")) {
                            throw new IllegalArgumentException("unknown option " + args[i]);
                        }
                        files.add(args[i]);
                }
            }
            if (files.isEmpty() || files.size() > 2) {
                throw new IllegalArgumentException("give a DWG file or folder, and an output if wanted");
            }
            if (o.format != null && !o.format.matches("png|svg|pdf")) {
                throw new IllegalArgumentException("format must be png, svg or pdf");
            }
            if ((o.size != 0 ? 1 : 0) + (o.pxPerUnit != 0 ? 1 : 0) + (o.dpi != 0 ? 1 : 0) > 1) {
                throw new IllegalArgumentException("give one of --size, --px-per-unit and --dpi");
            }
            if (!(o.scale > 0 && o.scale <= 64)) {
                throw new IllegalArgumentException("--scale must be more than 0 and at most 64, like 1/4 or 0.5");
            }
            if (o.size < 0 || o.pxPerUnit < 0 || o.dpi < 0 || o.tile < 16 || (o.size > 0 && o.size < 16)) {
                throw new IllegalArgumentException("sizes must be positive (--size and --tile at least 16)");
            }
        } catch (RuntimeException e) {
            String message = e instanceof ArrayIndexOutOfBoundsException ? "an option is missing its value"
                    : e instanceof NumberFormatException ? "a number was expected: " + e.getMessage().replaceFirst("^For input string: ", "")
                    : e.getMessage();
            System.err.println("error: " + message);
            usage();
            if (json) {
                Map<String, Object> doc = header(2);
                doc.put("error", message);
                printJson(doc, 2);
            }
            return 2;
        }

        Path input = Paths.get(files.get(0));
        Path output = files.size() > 1 ? Paths.get(files.get(1)) : null;
        List<Path> inputs;
        if (Files.isDirectory(input)) {
            try (Stream<Path> s = Files.walk(input)) {
                inputs = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".dwg"))
                          .sorted().collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("error: " + e.getMessage());
                if (json) {
                    Map<String, Object> doc = header(2);
                    doc.put("error", e.getMessage());
                    printJson(doc, 2);
                }
                return 2;
            }
        } else {
            inputs = List.of(input);
        }

        int failed = 0;
        List<Object> results = new ArrayList<>();
        for (Path in : inputs) {
            Path out = target(in, input, output, o.format);
            String fmt = o.format != null ? o.format : extension(out).isEmpty() ? "png" : extension(out);
            long start = System.nanoTime();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("input", in.toString());
            try {
                Result done = convert(in, out, fmt, o);
                long ms = (System.nanoTime() - start) / 1_000_000;
                if (json) {
                    r.put("output", out.toString());
                    r.put("ok", true);
                    r.putAll(done.json());
                    r.put("ms", ms);
                } else {
                    System.out.printf("ok      %s -> %s (%s, %d ms)%n", in, out, done.text(), ms);
                }
            } catch (Exception | OutOfMemoryError e) {
                failed++;
                if (json) {
                    r.put("output", out.toString());
                    r.put("ok", false);
                    r.put("error", message(e));
                } else {
                    System.out.printf("failed  %s: %s%n", in, message(e));
                }
            }
            results.add(r);
        }
        int status = failed == 0 ? 0 : 1;
        if (json) {
            Map<String, Object> doc = header(status);
            doc.put("converted", inputs.size() - failed);
            doc.put("failed", failed);
            doc.put("files", results);
            return reported(printJson(doc, status));
        }
        return reported(status);
    }

    /** After a failure, the line to put in a bug report. */
    private static int reported(int status) {
        if (status == 1) {
            System.err.println("To report a bug, send this line with the file and the message above: " + Version.describe());
        }
        return status;
    }

    /** Converts one file; returns what was made. */
    static Result convert(Path in, Path out, String format, Options o) throws IOException {
        DwgReader reader = DwgReader.defaultReader();
        if (o.maxBytes > 0) {
            reader.maxBytes(o.maxBytes);
        }
        // the objects go straight into the drawing as they are read: the document is not kept
        DocumentDrawing.Builder builder = new DocumentDrawing.Builder().keepObjects(false);
        reader.stream(in, builder);
        Drawing drawing = builder.build();

        Renderer renderer = new Renderer();
        Result result = new Result();
        result.format = format;
        result.sizing = size(renderer, drawing, o);
        result.entities = drawing.entityCount;
        result.decoded = drawing.decodedCount;
        result.failedToDecode = drawing.failedCount;
        if (o.white) {
            renderer.background = Color.WHITE;
            renderer.foreground = Color.BLACK;
        }
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        if (o.split && format.equals("png")) {
            split(renderer, drawing, out, o.tile, result);
            return result;
        }
        int[] size = renderer.imageSizeOf(drawing);
        if (size == null) throw new DwgReadException("nothing to draw");
        result.width = size[0];
        result.height = size[1];
        if (format.equals("svg")) {
            Files.write(out, renderer.renderSvg(drawing).getBytes(StandardCharsets.UTF_8));
            return result;
        }
        if (format.equals("pdf")) {
            Files.write(out, renderer.renderPdf(drawing));
            return result;
        }
        long need = 4L * size[0] * size[1];
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        if (need > free * 9 / 10 || need > Integer.MAX_VALUE - 8) {
            throw new DwgReadException(String.format("the image is %d x %d pixels (%d MB), more than the memory left;"
                    + " use --split, a --scale such as 1/4, or a larger -Xmx", size[0], size[1], need >> 20));
        }
        BufferedImage image = renderer.render(drawing);
        ImageIO.write(image, "png", out.toFile());
        result.width = image.getWidth();
        result.height = image.getHeight();
        return result;
    }

    /** {@code 1/4}, {@code 0.25} or {@code 2} as a number. */
    static double factor(String s) {
        int slash = s.indexOf('/');
        return slash < 0 ? Double.parseDouble(s)
                : Double.parseDouble(s.substring(0, slash).trim()) / Double.parseDouble(s.substring(slash + 1).trim());
    }

    /** Sets the size of the picture, then applies {@code --scale}; returns how it was chosen. */
    static String size(Renderer renderer, Drawing drawing, Options o) {
        String base = baseSize(renderer, drawing, o);
        if (o.scale == 1) {
            return base;
        }
        double f = o.scale;
        renderer.imageSize = (int) Math.max(16, Math.round(renderer.imageSize * f));
        renderer.scale *= f;
        renderer.fitWidth = (int) Math.round(renderer.fitWidth * f);
        renderer.fitHeight = (int) Math.round(renderer.fitHeight * f);
        renderer.margin = (int) Math.max(1, Math.round(renderer.margin * f));
        return base + " x " + scaleText(f);
    }

    private static String scaleText(double f) {
        double inverse = 1 / f;
        return f < 1 && Math.abs(inverse - Math.rint(inverse)) < 1e-9 ? "1/" + (long) Math.rint(inverse)
                : String.format(Locale.ROOT, "%s", f);
    }

    private static String baseSize(Renderer renderer, Drawing drawing, Options o) {
        if (o.size > 0) {
            renderer.imageSize = o.size;
            return "longer side " + o.size + " px";
        }
        if (o.pxPerUnit > 0) {
            renderer.scale = o.pxPerUnit;
            return o.pxPerUnit + " px per unit";
        }
        int dpi = o.dpi > 0 ? o.dpi : DEFAULT_DPI;
        if (drawing.paperWidth > 0 && drawing.paperHeight > 0) {
            renderer.fitWidth = (int) Math.round(drawing.paperWidth / 25.4 * dpi);
            renderer.fitHeight = (int) Math.round(drawing.paperHeight / 25.4 * dpi);
            return String.format(Locale.ROOT, "paper %.0f x %.0f mm at %d dpi", drawing.paperWidth, drawing.paperHeight, dpi);
        }
        if (o.dpi > 0) {
            throw new IllegalArgumentException("the drawing has no paper size for --dpi; use --px-per-unit or --size");
        }
        renderer.scale = 1;
        return "1 px per unit (no paper size in the drawing)";
    }

    /** Draws the PNG as tiles next to {@code out}, with a .tiles file that {@code merge} reads. */
    static void split(Renderer renderer, Drawing drawing, Path out, int tile, Result result) throws IOException {
        String base = out.getFileName().toString().replaceAll("(?i)\\.png$", "");
        Path dir = out.toAbsolutePath().getParent();
        int[] r = renderer.renderTiles(drawing, tile, BAND_BYTES,
                (row, column) -> new BufferedOutputStream(Files.newOutputStream(dir.resolve(tileName(base, row, column)))));
        if (r == null) throw new DwgReadException("nothing to draw");
        Properties p = new Properties();
        p.setProperty("width", Integer.toString(r[0]));
        p.setProperty("height", Integer.toString(r[1]));
        p.setProperty("rows", Integer.toString(r[2]));
        p.setProperty("columns", Integer.toString(r[3]));
        p.setProperty("tile", Integer.toString(tile));
        p.setProperty("name", base);
        try (Writer w = Files.newBufferedWriter(dir.resolve(base + ".tiles"), StandardCharsets.UTF_8)) {
            p.store(w, "tiles of " + base + ".png; join them with: java -jar jdwgparser.jar merge " + base + ".tiles");
        }
        result.width = r[0];
        result.height = r[1];
        result.tileRows = r[2];
        result.tileColumns = r[3];
        result.tilesFile = dir.resolve(base + ".tiles");
    }

    /** The tile in {@code column} (across) and {@code row} (down), both from 0, as {@code base[3x2].png} counted from 1. */
    static String tileName(String base, int row, int column) {
        return String.format("%s[%dx%d].png", base, column + 1, row + 1);
    }

    /** {@code merge <name>.tiles [out.png]}: the tiles as one PNG, a row of pixels at a time. */
    static int merge(String[] all, boolean json) {
        String[] args = Arrays.stream(all).filter(a -> !a.equals("--json")).toArray(String[]::new);
        if (args.length < 2 || args.length > 3) {
            System.err.println("usage: java -jar jdwgparser.jar merge <name>.tiles [out.png] [--json]");
            if (json) {
                Map<String, Object> doc = header(2);
                doc.put("error", "give a .tiles file, and an output if wanted");
                printJson(doc, 2);
            }
            return 2;
        }
        Path tiles = Paths.get(args[1]);
        long start = System.nanoTime();
        try {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(tiles, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            int width = Integer.parseInt(p.getProperty("width"));
            int height = Integer.parseInt(p.getProperty("height"));
            int rows = Integer.parseInt(p.getProperty("rows"));
            int columns = Integer.parseInt(p.getProperty("columns"));
            int tile = Integer.parseInt(p.getProperty("tile"));
            String base = p.getProperty("name");
            Path dir = tiles.toAbsolutePath().getParent();
            Path out = args.length > 2 ? Paths.get(args[2]) : dir.resolve(base + ".png");
            int[] line = new int[width];
            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out));
                 PngWriter writer = new PngWriter(os, width, height)) {
                for (int row = 0; row < rows; row++) {
                    int tileHeight = Math.min(tile, height - row * tile);
                    PngRows[] readers = new PngRows[columns];
                    try {
                        for (int c = 0; c < columns; c++) {
                            InputStream in = new BufferedInputStream(Files.newInputStream(dir.resolve(tileName(base, row, c))));
                            readers[c] = new PngRows(in);
                            int expected = Math.min(tile, width - c * tile);
                            if (readers[c].width() != expected || readers[c].height() != tileHeight) {
                                throw new IOException(tileName(base, row, c) + " is " + readers[c].width() + " x "
                                        + readers[c].height() + ", not " + expected + " x " + tileHeight);
                            }
                        }
                        for (int y = 0; y < tileHeight; y++) {
                            for (int c = 0; c < columns; c++) {
                                readers[c].readRow(line, c * tile);
                            }
                            writer.writeRow(line, 0);
                        }
                    } finally {
                        for (PngRows reader : readers) {
                            if (reader != null) reader.close();
                        }
                    }
                }
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (json) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("input", tiles.toString());
                r.put("output", out.toString());
                r.put("ok", true);
                r.put("format", "png");
                r.put("width", width);
                r.put("height", height);
                r.put("ms", ms);
                Map<String, Object> doc = header(0);
                doc.put("files", List.of(r));
                return printJson(doc, 0);
            }
            System.out.printf("ok      %s -> %s (%d x %d px, %d ms)%n", tiles, out, width, height, ms);
            return 0;
        } catch (Exception | OutOfMemoryError e) {
            if (json) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("input", tiles.toString());
                r.put("ok", false);
                r.put("error", message(e));
                Map<String, Object> doc = header(1);
                doc.put("files", List.of(r));
                return printJson(doc, 1);
            }
            System.out.printf("failed  %s: %s%n", tiles, message(e));
            return 1;
        }
    }

    private static String message(Throwable e) {
        if (e instanceof java.nio.file.NoSuchFileException) {
            return "file not found: " + e.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }

    /** The output of one input: given, in the given folder, or next to the input. */
    static Path target(Path in, Path inputRoot, Path output, String format) {
        String ext = format != null ? format : output != null && !Files.isDirectory(output) && !extension(output).isEmpty()
                ? extension(output) : "png";
        String name = in.getFileName().toString().replaceAll("(?i)\\.dwg$", "") + "." + ext;
        if (output == null) {
            return in.resolveSibling(name);
        }
        if (Files.isDirectory(output) || Files.isDirectory(inputRoot)) {
            Path relative = Files.isDirectory(inputRoot) ? inputRoot.relativize(in).resolveSibling(name) : Paths.get(name);
            return output.resolve(relative);
        }
        return output;
    }

    private static String extension(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        String e = dot < 0 ? "" : n.substring(dot + 1).toLowerCase(Locale.ROOT);
        return e.matches("png|svg|pdf") ? e : "";
    }

    private static void usage() {
        System.err.println(String.join("\n",
                "usage: java -jar jdwgparser.jar <file.dwg|folder> [output] [options]",
                "       java -jar jdwgparser.jar merge <name>.tiles [out.png]",
                "       java -jar jdwgparser.jar --version",
                "  output        a file (.png, .svg, .pdf) or a folder; default: next to the input, .png",
                "  --format F    png, svg or pdf",
                "size (one of them; default: the drawing's paper at 300 dpi, else 1 px per unit):",
                "  --dpi N       the drawing's paper at N dots per inch",
                "  --px-per-unit N  N pixels per drawing unit (1 = the drawing's own size)",
                "  --size N      N pixels on the longer side",
                "  --scale F     then shrink (or enlarge) by F: 1/2, 1/4, 1/8, 1/16, 0.3 ...",
                "PNG:",
                "  --split       tiles drawn a band at a time: little memory, slower (join them with merge)",
                "  --tile N      tile size in pixels for --split (default 2048)",
                "  --background  black (default) or white",
                "  --max-mb N    memory one file may use for its sections, in MB",
                "  --json        print one JSON document instead of the result lines (also for merge, --version)",
                LICENSE_LINE));
    }
}
