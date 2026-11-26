package io.dwg.render;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Text as outlines, in drawing units with Y up and the baseline start at the origin.
 * A system font is used for every text style; SHX and TrueType fonts of the drawing are not resolved.
 */
final class TextShapes {
    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 100);
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    /** height of a capital letter in font units; the text height of a drawing is the capital height */
    private static final double CAP_HEIGHT = FONT.createGlyphVector(FRC, "H").getVisualBounds().getHeight();
    /** distance between the baselines of MTEXT lines, relative to the text height */
    private static final double LINE_SPACING = 5.0 / 3.0;

    private TextShapes() {
    }

    static double width(String s, double height, double widthFactor) {
        if (s.isEmpty()) {
            return 0.0;
        }
        return FONT.createGlyphVector(FRC, s).getLogicalBounds().getWidth() * height / CAP_HEIGHT * widthFactor;
    }

    /** One line of text; oblique is the slant angle in radians. */
    static Shape line(String s, double height, double widthFactor, double oblique) {
        GlyphVector gv = FONT.createGlyphVector(FRC, s);
        double k = height / CAP_HEIGHT;
        // font units (Y down) -> drawing units (Y up), with width factor and slant
        AffineTransform at = new AffineTransform(k * widthFactor, 0.0, -k * Math.tan(oblique), -k, 0.0, 0.0);
        return at.createTransformedShape(gv.getOutline());
    }

    /**
     * Single line TEXT, positioned so that its alignment point is at the origin.
     * @param hAlign 0 left, 1 center, 2 right, 3 aligned, 4 middle, 5 fit
     * @param vAlign 0 baseline, 1 bottom, 2 middle, 3 top
     */
    static Shape aligned(String s, double height, double widthFactor, double oblique, int hAlign, int vAlign) {
        double w = width(s, height, widthFactor);
        double ax = hAlign == 1 || hAlign == 4 ? w / 2.0 : hAlign == 2 ? w : 0.0;
        double ay = vAlign == 3 ? height : vAlign == 2 ? height / 2.0 : vAlign == 1 ? -0.2 * height : 0.0;
        if (hAlign == 4 && vAlign == 0) {
            ay = height / 2.0;
        }
        Path2D.Double path = new Path2D.Double(line(s, height, widthFactor, oblique));
        path.transform(AffineTransform.getTranslateInstance(-ax, -ay));
        return path;
    }

    /**
     * MTEXT paragraph, positioned so that its attachment point is at the origin.
     * @param attachment 1-3 top, 4-6 middle, 7-9 bottom; left, center, right
     * @param rectWidth  width to wrap at, 0 for no wrapping
     */
    static Shape paragraph(String content, double height, int attachment, double rectWidth) {
        List<String> lines = new ArrayList<>();
        for (String line : plainLines(content)) {
            wrap(line, height, rectWidth, lines);
        }
        int column = attachment >= 1 && attachment <= 9 ? (attachment - 1) % 3 : 0;
        int row = attachment >= 1 && attachment <= 9 ? (attachment - 1) / 3 : 0;
        double spacing = height * LINE_SPACING;
        double total = height + (lines.size() - 1) * spacing;
        double top = row == 0 ? 0.0 : row == 1 ? total / 2.0 : total;

        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.trim().isEmpty()) {
                continue;
            }
            double x = -width(s, height, 1.0) * column / 2.0;
            double y = top - height - i * spacing;
            path.append(AffineTransform.getTranslateInstance(x, y).createTransformedShape(line(s, height, 1.0, 0.0)), false);
        }
        return path;
    }

    private static void wrap(String line, double height, double rectWidth, List<String> out) {
        if (rectWidth <= 0.0 || width(line, height, 1.0) <= rectWidth) {
            out.add(line);
            return;
        }
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && width(candidate, height, 1.0) > rectWidth) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        out.add(current.toString());
    }

    /** Single line text without its control codes ({@link io.dwg.api.PlainText#ofText}). */
    static String plain(String s) {
        return io.dwg.api.PlainText.ofText(s);
    }

    /** Lines of an MTEXT content without its formatting codes ({@link io.dwg.api.PlainText#linesOfMText}). */
    static List<String> plainLines(String s) {
        return io.dwg.api.PlainText.linesOfMText(s);
    }
}
