package io.dwg.render;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Vector outputs of the {@link Renderer}: SVG and PDF. Both get the shapes in output coordinates
 * (y down, as in the PNG output) and write them as paths; lines are one unit wide, with round
 * caps and joins.
 */
final class VectorCanvas {

    private VectorCanvas() {
    }

    /** A number with at most 3 decimals and no trailing zeros. */
    static String num(double v) {
        if (Math.abs(v) < 0.0005) {
            return "0";
        }
        String s = String.format(Locale.ROOT, "%.3f", v);
        int end = s.length();
        while (s.charAt(end - 1) == '0') end--;
        if (s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }

    /** SVG 1.1. */
    static final class Svg implements Renderer.Canvas {
        private final StringBuilder out = new StringBuilder();
        private int clips;

        Svg(int width, int height, Color background) {
            out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"").append(width)
               .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(' ')
               .append(height).append("\">\n");
            out.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(hex(background)).append("\"/>\n");
            out.append("<g stroke-width=\"1\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n");
        }

        @Override
        public void fill(Shape s, Color c) {
            String d = pathData(s);
            if (d.isEmpty()) return;
            out.append("<path d=\"").append(d).append("\" fill=\"").append(hex(c)).append('"');
            if (c.getAlpha() < 255) {
                out.append(" fill-opacity=\"").append(num(c.getAlpha() / 255.0)).append('"');
            }
            if (s.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                out.append(" fill-rule=\"evenodd\"");
            }
            out.append(" stroke=\"none\"/>\n");
        }

        @Override
        public void draw(Shape s, Color c) {
            draw(s, c, null);
        }

        private void draw(Shape s, Color c, String clip) {
            String d = pathData(s);
            if (d.isEmpty()) return;
            out.append("<path d=\"").append(d).append("\" fill=\"none\" stroke=\"").append(hex(c)).append('"');
            if (c.getAlpha() < 255) {
                out.append(" stroke-opacity=\"").append(num(c.getAlpha() / 255.0)).append('"');
            }
            if (clip != null) {
                out.append(" clip-path=\"url(#").append(clip).append(")\"");
            }
            out.append("/>\n");
        }

        @Override
        public void drawClipped(Shape lines, Shape clip, Color c) {
            String id = "c" + (++clips);
            out.append("<clipPath id=\"").append(id).append("\"><path d=\"").append(pathData(clip)).append('"');
            if (clip.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                out.append(" clip-rule=\"evenodd\"");
            }
            out.append("/></clipPath>\n");
            draw(lines, c, id);
        }

        String finish() {
            out.append("</g>\n</svg>\n");
            return out.toString();
        }

        private static String hex(Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }

        private static String pathData(Shape s) {
            StringBuilder d = new StringBuilder();
            double[] p = new double[6];
            for (PathIterator it = s.getPathIterator(null); !it.isDone(); it.next()) {
                switch (it.currentSegment(p)) {
                    case PathIterator.SEG_MOVETO:
                        d.append('M').append(num(p[0])).append(' ').append(num(p[1]));
                        break;
                    case PathIterator.SEG_LINETO:
                        d.append('L').append(num(p[0])).append(' ').append(num(p[1]));
                        break;
                    case PathIterator.SEG_QUADTO:
                        d.append('Q').append(num(p[0])).append(' ').append(num(p[1])).append(' ')
                         .append(num(p[2])).append(' ').append(num(p[3]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        d.append('C').append(num(p[0])).append(' ').append(num(p[1])).append(' ')
                         .append(num(p[2])).append(' ').append(num(p[3])).append(' ')
                         .append(num(p[4])).append(' ').append(num(p[5]));
                        break;
                    default:
                        d.append('Z');
                        break;
                }
            }
            return d.toString();
        }
    }

    /**
     * PDF 1.4, one page: a catalog, the page tree, the page, its content stream (Flate) and the
     * cross-reference table. Translucent fills use an ExtGState per opacity.
     */
    static final class Pdf implements Renderer.Canvas {
        private final StringBuilder content = new StringBuilder();
        private final int width;
        private final int height;
        private final Map<Integer, String> opacities = new LinkedHashMap<>();

        Pdf(int width, int height, Color background) {
            this.width = width;
            this.height = height;
            content.append("1 w 1 J 1 j\n");
            color(background, "rg");
            content.append("0 0 ").append(width).append(' ').append(height).append(" re f\n");
        }

        @Override
        public void fill(Shape s, Color c) {
            content.append("q\n");
            alpha(c);
            color(c, "rg");
            path(s);
            content.append(evenOdd(s) ? "f*\n" : "f\n");
            content.append("Q\n");
        }

        @Override
        public void draw(Shape s, Color c) {
            color(c, "RG");
            path(s);
            content.append("S\n");
        }

        @Override
        public void drawClipped(Shape lines, Shape clip, Color c) {
            content.append("q\n");
            path(clip);
            content.append(evenOdd(clip) ? "W* n\n" : "W n\n");
            draw(lines, c);
            content.append("Q\n");
        }

        private void alpha(Color c) {
            if (c.getAlpha() < 255) {
                String name = opacities.computeIfAbsent(c.getAlpha(), a -> "GS" + (opacities.size() + 1));
                content.append('/').append(name).append(" gs\n");
            }
        }

        private void color(Color c, String op) {
            content.append(num(c.getRed() / 255.0)).append(' ').append(num(c.getGreen() / 255.0)).append(' ')
                   .append(num(c.getBlue() / 255.0)).append(' ').append(op).append('\n');
        }

        private static boolean evenOdd(Shape s) {
            return s.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD;
        }

        /** The path in PDF coordinates (y up); quadratic segments become cubic ones. */
        private void path(Shape s) {
            double[] p = new double[6];
            double x = 0, y = 0;
            for (PathIterator it = s.getPathIterator(null); !it.isDone(); it.next()) {
                switch (it.currentSegment(p)) {
                    case PathIterator.SEG_MOVETO:
                        point(p[0], p[1]).append("m\n");
                        x = p[0];
                        y = p[1];
                        break;
                    case PathIterator.SEG_LINETO:
                        point(p[0], p[1]).append("l\n");
                        x = p[0];
                        y = p[1];
                        break;
                    case PathIterator.SEG_QUADTO: {
                        double c1x = x + 2.0 / 3.0 * (p[0] - x), c1y = y + 2.0 / 3.0 * (p[1] - y);
                        double c2x = p[2] + 2.0 / 3.0 * (p[0] - p[2]), c2y = p[3] + 2.0 / 3.0 * (p[1] - p[3]);
                        point(c1x, c1y);
                        point(c2x, c2y);
                        point(p[2], p[3]).append("c\n");
                        x = p[2];
                        y = p[3];
                        break;
                    }
                    case PathIterator.SEG_CUBICTO:
                        point(p[0], p[1]);
                        point(p[2], p[3]);
                        point(p[4], p[5]).append("c\n");
                        x = p[4];
                        y = p[5];
                        break;
                    default:
                        content.append("h\n");
                        break;
                }
            }
        }

        private StringBuilder point(double px, double py) {
            return content.append(num(px)).append(' ').append(num(height - py)).append(' ');
        }

        byte[] finish() {
            byte[] raw = content.toString().getBytes(StandardCharsets.US_ASCII);
            Deflater deflater = new Deflater();
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream packed = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            while (!deflater.finished()) {
                packed.write(buf, 0, deflater.deflate(buf));
            }
            deflater.end();
            byte[] stream = packed.toByteArray();

            StringBuilder gs = new StringBuilder();
            opacities.forEach((a, name) -> gs.append('/').append(name).append(" << /Type /ExtGState /ca ")
                    .append(num(a / 255.0)).append(" >> "));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            long[] offsets = new long[6];
            write(out, "%PDF-1.4\n%âãÏÓ\n");
            offsets[1] = out.size();
            write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            offsets[2] = out.size();
            write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
            offsets[3] = out.size();
            write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + width + " " + height + "]"
                    + " /Resources << /ExtGState << " + gs + ">> >> /Contents 4 0 R >>\nendobj\n");
            offsets[4] = out.size();
            write(out, "4 0 obj\n<< /Length " + stream.length + " /Filter /FlateDecode >>\nstream\n");
            out.write(stream, 0, stream.length);
            write(out, "\nendstream\nendobj\n");
            long xref = out.size();
            StringBuilder table = new StringBuilder("xref\n0 5\n0000000000 65535 f \n");
            for (int i = 1; i <= 4; i++) {
                table.append(String.format("%010d 00000 n \n", offsets[i]));
            }
            table.append("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
            write(out, table.toString());
            return out.toByteArray();
        }

        private static void write(ByteArrayOutputStream out, String s) {
            byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
            out.write(b, 0, b.length);
        }
    }
}
