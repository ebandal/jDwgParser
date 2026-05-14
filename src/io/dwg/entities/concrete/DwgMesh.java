package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * MESH 엔티티 - 자유형 3D 메시 (R2007+)
 */
public class DwgMesh extends AbstractDwgEntity {
    private int version;  // Mesh version
    private int numVertices;
    private int numFaces;
    private double[][] vertices;  // Array of 3D vertices
    private int[][] faces;        // Array of face vertex indices
    private int creasesCount;
    private int[] creases;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MESH; }

    public int version() { return version; }
    public int numVertices() { return numVertices; }
    public int numFaces() { return numFaces; }
    public double[][] vertices() { return vertices; }
    public int[][] faces() { return faces; }
    public int creasesCount() { return creasesCount; }
    public int[] creases() { return creases; }

    public void setVersion(int version) { this.version = version; }
    public void setNumVertices(int numVertices) { this.numVertices = numVertices; }
    public void setNumFaces(int numFaces) { this.numFaces = numFaces; }
    public void setVertices(double[][] vertices) { this.vertices = vertices; }
    public void setFaces(int[][] faces) { this.faces = faces; }
    public void setCreasesCount(int creasesCount) { this.creasesCount = creasesCount; }
    public void setCreases(int[] creases) { this.creases = creases; }
}
