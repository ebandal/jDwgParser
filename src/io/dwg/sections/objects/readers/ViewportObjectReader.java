package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgViewport;
import io.dwg.sections.objects.ObjectReader;

/**
 * VIEWPORT 엔티티 리더 (타입 0x22)
 * 레이아웃의 뷰포트
 */
public class ViewportObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VIEWPORT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgViewport viewport = (DwgViewport) target;

        // 중심점 (3D)
        double[] center = r.read3BitDouble();
        viewport.setCenter(new Point3D(center[0], center[1], center[2]));

        // 폭
        double width = r.getInput().readRawDouble();
        viewport.setWidth(width);

        // 높이
        double height = r.getInput().readRawDouble();
        viewport.setHeight(height);

        // 뷰 높이
        double viewHeight = r.getInput().readRawDouble();
        viewport.setViewHeight(viewHeight);

        // 노멀 벡터
        double[] extrusion = r.readBitExtrusion();
        viewport.setExtrusion(extrusion);
    }
}
