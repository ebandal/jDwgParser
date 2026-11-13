package structure.entity;

public class MText extends Entity {
    public double[] insertion;                  // WCS
    public double[] extrusion;
    public double[] xAxis;                      // WCS direction of the text
    public double rectWidth;
    public double height;
    public int attachment;                      // 1-3 top, 4-6 middle, 7-9 bottom; left, center, right
    public String text;                         // with formatting codes
}
