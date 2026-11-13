package io.dwg.render.model.entity;

import java.util.ArrayList;
import java.util.List;

/** POLYLINE (polyface mesh) with its VERTEX entities. */
public class PolyfaceMesh extends Entity {
    public int numVertices;
    public int numFaces;
    public List<double[]> vertices = new ArrayList<>();     // WCS
    public List<int[]> faces = new ArrayList<>();           // 1-based vertex indexes, negative = invisible edge, 0 = unused
}
