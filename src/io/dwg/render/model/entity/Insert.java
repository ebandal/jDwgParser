package io.dwg.render.model.entity;

import java.util.ArrayList;
import java.util.List;

/** Block reference. */
public class Insert extends Entity {
    public double[] insertion;                  // OCS
    public double[] scale = { 1.0, 1.0, 1.0 };
    public double rotation;                     // radians
    public double[] extrusion;
    public boolean hasAttribs;
    public long blockHandle;                    // BLOCK_HEADER
    public List<Text> attribs = new ArrayList<>();
}
