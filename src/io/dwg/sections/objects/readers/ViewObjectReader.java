package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgView;
import io.dwg.sections.objects.ObjectReader;

/**
 * VIEW (저장된 뷰) 테이블 엔트리 ObjectReader.
 * 타입 0x36
 */
public class ViewObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VIEW.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgView view = (DwgView) target;

        view.setName(r.readVariableText());

        double[] target3d = r.read3BitDouble();
        view.setViewTarget(new Point3D(target3d[0], target3d[1], target3d[2]));

        double[] direction3d = r.read3BitDouble();
        view.setViewDirection(new Point3D(direction3d[0], direction3d[1], direction3d[2]));

        view.setViewHeight(r.readBitDouble());
        view.setViewWidth(r.readBitDouble());
    }
}
