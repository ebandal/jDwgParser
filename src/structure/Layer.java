package structure;

public class Layer {
    public long handle;
    public String name;
    public int color;           // color index
    public boolean frozen;
    public boolean off;

    public boolean isLayerZero() {
        return "0".equals(name);
    }

    public boolean isHidden() {
        return frozen || off;
    }
}
