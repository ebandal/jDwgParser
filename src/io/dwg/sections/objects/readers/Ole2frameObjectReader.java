package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgOle2frame;
import io.dwg.sections.objects.ObjectReader;

/**
 * OLE2FRAME 엔티티 리더 (타입 0x3E)
 * OLE 객체 임베딩
 */
public class Ole2frameObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.OLE2FRAME.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgOle2frame ole = (DwgOle2frame) target;

        // 1. insertionPoint (3RD)
        double[] pt = r.read3RawDouble();
        ole.setInsertionPoint(new Point3D(pt[0], pt[1], pt[2]));

        // 2. scale (3RD)
        double[] scale = r.read3BitDouble();
        ole.setScale(scale);

        // 3. rotation (BD)
        double rotation = r.readBitDouble();
        ole.setRotation(rotation);

        // 4. oleName (TU)
        String oleName = r.readVariableText();
        ole.setOleName(oleName);

        // 5. oleData (variable length binary)
        long dataLength = r.readBitLong();
        byte[] oleData = new byte[(int)dataLength];
        for (int i = 0; i < dataLength; i++) {
            oleData[i] = (byte) r.getInput().readBits(8);
        }
        ole.setOleData(oleData);
    }
}
