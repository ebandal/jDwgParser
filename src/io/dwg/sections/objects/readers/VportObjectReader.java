package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVport;
import io.dwg.sections.objects.ObjectReader;

/**
 * VPORT (뷰포트 설정) 테이블 엔트리 ObjectReader.
 * 타입 0x38
 */
public class VportObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.VPORT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVport vport = (DwgVport) target;

        vport.setName(r.readVariableText());

        double[] center2d = r.read2BitDouble();
        vport.setViewCenter(new Point3D(center2d[0], center2d[1], 0));

        double[] snapBase2d = r.read2BitDouble();
        vport.setSnapBase(new Point3D(snapBase2d[0], snapBase2d[1], 0));

        vport.setGridSpacingX(r.readBitDouble());
        vport.setGridSpacingY(r.readBitDouble());
    }
}
