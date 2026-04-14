package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolylinePface;
import io.dwg.sections.objects.ObjectReader;

/**
 * POLYLINE_PFACE 엔티티 리더 (타입 0x1D)
 * 면 폴리라인 (얼굴의 집합)
 */
public class PolylinePfaceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POLYLINE_PFACE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPolylinePface polyline = (DwgPolylinePface) target;

        // 1. numVertices (BS)
        int numVertices = r.readBitShort();
        polyline.setNumVertices(numVertices);

        // 2. numFaces (BS)
        int numFaces = r.readBitShort();
        polyline.setNumFaces(numFaces);

        // 3. extrusion (BE)
        double[] extrusion = r.readBitExtrusion();
        polyline.setExtrusion(extrusion);
    }
}
