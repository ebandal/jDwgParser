package structure;

public class BlockHeader {
    public long handle;
    public String name;
    public boolean anonymous;
    public boolean xref;
    public double[] basePoint;

    public boolean isModelSpace() {
        return "*MODEL_SPACE".equalsIgnoreCase(name);
    }
}
