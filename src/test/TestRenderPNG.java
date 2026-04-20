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

        // Calculate bounds of all entities
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        if (dwg.parsedObjects != null && !dwg.parsedObjects.isEmpty()) {
            for (DwgObject obj : dwg.parsedObjects.values()) {
                try {
                    double[] bounds = getEntityBounds(obj);
                    if (bounds != null) {
                        minX = Math.min(minX, bounds[0]);
                        minY = Math.min(minY, bounds[1]);
                        maxX = Math.max(maxX, bounds[2]);
                        maxY = Math.max(maxY, bounds[3]);
                    }
                } catch (Exception e) {
                    // Ignore bounds calculation errors
                }
            }
        }

        // Calculate scale to fit all entities
        double boundsWidth = maxX - minX;
        double boundsHeight = maxY - minY;
        double scale = SCALE;
        double offsetX = MARGIN;
        double offsetY = HEIGHT - MARGIN;

        if (boundsWidth > 0 && boundsHeight > 0) {
            double scaleX = (WIDTH - 2 * MARGIN) / boundsWidth;
            double scaleY = (HEIGHT - 2 * MARGIN) / boundsHeight;
            scale = Math.min(scaleX, scaleY);
            offsetX = MARGIN - minX * scale;
            offsetY = HEIGHT - MARGIN + minY * scale;
        }

        System.out.println("Bounds: (" + minX + ", " + minY + ") to (" + maxX + ", " + maxY + ")");
        System.out.println("Scale: " + scale);

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
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, -scale); // Flip Y axis for DWG coordinates

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

    private static double[] getEntityBounds(DwgObject obj) {
        try {
            if (obj instanceof DwgLine) {
                DwgLine line = (DwgLine) obj;
                double x1 = line.start().getX(), y1 = line.start().getY();
                double x2 = line.end().getX(), y2 = line.end().getY();
                return new double[]{Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2)};
            } else if (obj instanceof DwgCircle) {
                DwgCircle circle = (DwgCircle) obj;
                double cx = circle.center().getX(), cy = circle.center().getY(), r = circle.radius();
                return new double[]{cx - r, cy - r, cx + r, cy + r};
            } else if (obj instanceof DwgArc) {
                DwgArc arc = (DwgArc) obj;
                double cx = arc.center().getX(), cy = arc.center().getY(), r = arc.radius();
                return new double[]{cx - r, cy - r, cx + r, cy + r};
            } else if (obj instanceof DwgEllipse) {
                DwgEllipse ellipse = (DwgEllipse) obj;
                double cx = ellipse.center().getX(), cy = ellipse.center().getY();
                double major = ellipse.majorRadius(), minor = ellipse.minorRadius();
                return new double[]{cx - major, cy - minor, cx + major, cy + minor};
            } else if (obj instanceof DwgPoint) {
                DwgPoint point = (DwgPoint) obj;
                double x = point.position().getX(), y = point.position().getY();
                return new double[]{x - 1, y - 1, x + 1, y + 1};
            }
        } catch (Exception e) {
            // Ignore bounds errors
        }
        return null;
    }

    private static boolean renderEntity(Graphics2D g2d, DwgObject obj) {
        if (obj instanceof DwgLine) {
            return renderLine(g2d, (DwgLine) obj);
        } else if (obj instanceof DwgCircle) {
            return renderCircle(g2d, (DwgCircle) obj);
        } else if (obj instanceof DwgArc) {
            return renderArc(g2d, (DwgArc) obj);
        } else if (obj instanceof DwgEllipse) {
            return renderEllipse(g2d, (DwgEllipse) obj);
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

    private static boolean renderEllipse(Graphics2D g2d, DwgEllipse ellipse) {
        try {
            g2d.setColor(new Color(0, 128, 255)); // Light blue
            g2d.setStroke(new BasicStroke(0.1f));

            double cx = ellipse.center().getX();
            double cy = ellipse.center().getY();

            // Get major and minor axis radii
            double majorLen = ellipse.majorRadius();
            double minorLen = ellipse.minorRadius();

            // Get rotation angle from major axis vector
            structure.entities.Point3D majorAxis = ellipse.majorAxisVec();
            double angle = Math.atan2(majorAxis.getY(), majorAxis.getX());

            // Create ellipse in standard position
            Ellipse2D.Double ellipse2D = new Ellipse2D.Double(
                    cx - majorLen, cy - minorLen, majorLen * 2, minorLen * 2);

            // Apply rotation transform
            AffineTransform transform = AffineTransform.getRotateInstance(angle, cx, cy);
            Shape rotated = transform.createTransformedShape(ellipse2D);

            g2d.draw(rotated);
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
