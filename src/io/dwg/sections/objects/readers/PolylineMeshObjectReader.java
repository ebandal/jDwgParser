package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgPolyline;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolylineMesh;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * POLYLINE (MESH) (type 30), spec §20.4.34: flags BS, curve type BS, M and N vertex counts BS,
 * M and N densities BS, R2004+ owned object count BL; handles as {@link PolylineHandles}.
 */
public class PolylineMeshObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POLYLINE_MESH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgPolylineMesh polyline = (DwgPolylineMesh) target;
        polyline.setFlags(r.readBitShort());
        polyline.setCurveType(r.readBitShort());
        polyline.setMVertexCount(r.readBitShort());
        polyline.setNVertexCount(r.readBitShort());
        polyline.setMDensity(r.readBitShort());
        polyline.setNDensity(r.readBitShort());
        PolylineHandles.readOwnedCount(polyline, r, v);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        PolylineHandles.read((AbstractDwgPolyline) target, r, v);
    }
}
