package test;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import structure.Dwg;
import structure.entities.*;
import decode.DwgParseException;

public class TestRenderPNG {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;
    private static final double SCALE = 10.0; // Scale factor for drawing
    private static final int MARGIN = 50;

    public static void main(String[] args) throws Exception {
        // Enable logging
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (java.util.logging.Handler h : root.getHandlers()) h.setLevel(Level.INFO);

        String path = args.length > 0 ? args[0] : "samples/example_2004.dwg";
        String outputPath = args.length > 1 ? args[1] : "target/render_output.png";

        File f = new File(path);
        System.out.println("=== Rendering DWG to PNG ===");
        System.out.println("Input:  " + f.getAbsolutePath());
        System.out.println("Output: " + outputPath);
        System.out.println();

        // Parse DWG
        Dwg dwg = new Dwg();
        try {
            dwg.decode(f);
        } catch (DwgParseException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Version: " + dwg.header.ver);
        System.out.println("Parsed objects: " + (dwg.parsedObjects == null ? 0 : dwg.parsedObjects.size()));
        System.out.println();

        // Create image
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Clear with white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Setup coordinate system
        g2d.translate(MARGIN, HEIGHT - MARGIN);
        g2d.scale(SCALE, -SCALE); // Flip Y axis for DWG coordinates

        // Render objects
        int renderedCount = 0;
        if (dwg.parsedObjects != null && !dwg.parsedObjects.isEmpty()) {
            for (DwgObject obj : dwg.parsedObjects.values()) {
                try {
                    if (renderEntity(g2d, obj)) {
                        renderedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error rendering " + obj.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        g2d.dispose();

        // Save PNG
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", outputFile);

        System.out.println("=== Rendering Complete ===");
        System.out.println("Rendered " + renderedCount + " entities");
        System.out.println("Image size: " + WIDTH + "x" + HEIGHT);
        System.out.println("Saved to: " + outputFile.getAbsolutePath());
    }

    private static boolean renderEntity(Graphics2D g2d, DwgObject obj) {
        if (obj instanceof DwgLine) {
            return renderLine(g2d, (DwgLine) obj);
        } else if (obj instanceof DwgCircle) {
            return renderCircle(g2d, (DwgCircle) obj);
        } else if (obj instanceof DwgArc) {
            return renderArc(g2d, (DwgArc) obj);
        } else if (obj instanceof DwgText) {
            return renderText(g2d, (DwgText) obj);
        } else if (obj instanceof DwgLwPolyline) {
            return renderLwPolyline(g2d, (DwgLwPolyline) obj);
        } else if (obj instanceof DwgPoint) {
            return renderPoint(g2d, (DwgPoint) obj);
        }
        return false;
    }

    private static boolean renderLine(Graphics2D g2d, DwgLine line) {
        try {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(0.1f));

            double x1 = line.start().getX();
            double y1 = line.start().getY();
            double x2 = line.end().getX();
            double y2 = line.end().getY();

            g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean renderCircle(Graphics2D g2d, DwgCircle circle) {
        try {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(0.1f));

            double cx = circle.center().getX();
            double cy = circle.center().getY();
            double r = circle.radius();

            g2d.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean renderArc(Graphics2D g2d, DwgArc arc) {
        try {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(0.1f));

            double cx = arc.center().getX();
            double cy = arc.center().getY();
            double r = arc.radius();
            double startAngle = arc.startAngle();
            double endAngle = arc.endAngle();

            Arc2D arc2D = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
                    Math.toDegrees(startAngle), Math.toDegrees(endAngle - startAngle),
                    Arc2D.OPEN);
            g2d.draw(arc2D);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean renderText(Graphics2D g2d, DwgText text) {
        try {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            double x = text.insertionPoint().getX();
            double y = text.insertionPoint().getY();
            String content = text.value();

            if (content != null && !content.isEmpty()) {
                g2d.drawString(content, (int) x, (int) y);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean renderLwPolyline(Graphics2D g2d, DwgLwPolyline polyline) {
        try {
            g2d.setColor(new Color(128, 0, 128)); // Purple
            g2d.setStroke(new BasicStroke(0.1f));

            GeneralPath path = new GeneralPath();
            boolean first = true;

            for (structure.entities.Point2D pt : polyline.vertices()) {
                if (first) {
                    path.moveTo(pt.getX(), pt.getY());
                    first = false;
                } else {
                    path.lineTo(pt.getX(), pt.getY());
                }
            }

            g2d.draw(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean renderPoint(Graphics2D g2d, DwgPoint point) {
        try {
            g2d.setColor(Color.GRAY);

            double x = point.position().getX();
            double y = point.position().getY();
            int size = 2;

            g2d.fillOval((int) (x - size), (int) (y - size), size * 2, size * 2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
