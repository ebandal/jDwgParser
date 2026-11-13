package structure.entity;

import structure.DwgObject;

/** Common entity data (DWG specification, "Common entity format"). */
public abstract class Entity {
    public static final int MODE_OWNED = 0;         // owner handle is in the handle stream (inside a block)
    public static final int MODE_PAPER_SPACE = 1;
    public static final int MODE_MODEL_SPACE = 2;

    public static final int COLOR_BYBLOCK = 0;
    public static final int COLOR_BYLAYER = 256;

    public DwgObject object;
    public int entMode;
    public int numReactors;
    public boolean noLinks;
    public int color;               // color index; 0 = BYBLOCK, 256 = BYLAYER
    public double ltypeScale;
    public int ltypeFlags;
    public int plotstyleFlags;
    public boolean invisible;
    public int lineweight;

    public long ownerHandle;        // BLOCK_HEADER (or parent entity) when entMode is MODE_OWNED, else 0
    public long layerHandle;

    public boolean inModelSpace() {
        return entMode == MODE_MODEL_SPACE;
    }
}
