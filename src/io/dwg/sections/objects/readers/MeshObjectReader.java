package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMesh;
import io.dwg.sections.objects.ObjectReader;

public class MeshObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MESH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMesh mesh = (DwgMesh) target;

        // Mesh version
        mesh.setVersion(r.readBitShort());

        // Number of vertices and faces
        int numVertices = r.readBitShort();
        int numFaces = r.readBitShort();
        mesh.setNumVertices(numVertices);
        mesh.setNumFaces(numFaces);

        // Read vertices
        double[][] vertices = new double[numVertices][3];
        for (int i = 0; i < numVertices; i++) {
            double[] vertex = r.read3BitDouble();
            vertices[i][0] = vertex[0];
            vertices[i][1] = vertex[1];
            vertices[i][2] = vertex[2];
        }
        mesh.setVertices(vertices);

        // Read faces (simplified: store vertex indices)
        int[][] faces = new int[numFaces][];
        for (int i = 0; i < numFaces; i++) {
            int vertexCount = r.readBitShort();
            faces[i] = new int[vertexCount];
            for (int j = 0; j < vertexCount; j++) {
                faces[i][j] = r.readBitShort();
            }
        }
        mesh.setFaces(faces);

        // Read crease information
        int creasesCount = r.readBitShort();
        mesh.setCreasesCount(creasesCount);
        if (creasesCount > 0) {
            int[] creases = new int[creasesCount];
            for (int i = 0; i < creasesCount; i++) {
                creases[i] = r.readBitShort();
            }
            mesh.setCreases(creases);
        }
    }
}
