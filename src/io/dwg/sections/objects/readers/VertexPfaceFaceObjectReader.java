package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexPfaceFace;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * VERTEX_PFACE_FACE 엔티티 리더 (타입 0x0E)
 * 면 폴리라인의 면 정의
 */
public class VertexPfaceFaceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VERTEX_PFACE_FACE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        // 1부터 시작하는 꼭지점 번호. 음수는 그 꼭지점에서 시작하는 모서리가 보이지 않음을 뜻한다.
        DwgVertexPfaceFace vertex = (DwgVertexPfaceFace) target;
        vertex.setVertex1Index((short) r.readBitShort());
        vertex.setVertex2Index((short) r.readBitShort());
        vertex.setVertex3Index((short) r.readBitShort());
        vertex.setVertex4Index((short) r.readBitShort());
    }
}
