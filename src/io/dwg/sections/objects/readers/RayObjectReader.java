package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgRay;
import io.dwg.sections.objects.ObjectReader;

/**
 * RAY 엔티티 리더 (타입 0x28)
 * 시작점과 방향으로 정의된 반무한 선
 */
public class RayObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.RAY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgRay ray = (DwgRay) target;

        // 시작점 (3D, 압축됨)
        double[] start = r.read3RawDouble();
        ray.setStart(new Point3D(start[0], start[1], start[2]));

        // 방향 벡터 (3D, 단위 벡터)
        double[] direction = r.read3RawDouble();
        ray.setDirection(new Point3D(direction[0], direction[1], direction[2]));
    }
}
