package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.sections.objects.ObjectReader;

public class LayerObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LAYER.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayer layer = (DwgLayer) target;
        layer.setName(r.readVariableText());
        // 테이블 레코드 공통 : 64-flag, xref index + 1, xdep
        r.getInput().readBit();
        r.readBitShort();
        r.getInput().readBit();

        int flags = r.readBitShort();
        layer.setFlags(flags);
        layer.setFrozen((flags & 0x01) != 0);
        layer.setOn((flags & 0x02) == 0);
        layer.setFrozenInNewViewports((flags & 0x04) != 0);
        layer.setLocked((flags & 0x08) != 0);

        int[] colorData = r.readCmColor();
        int colorIndex = colorData[0];
        if (colorData.length > 1) {
            // R2004+ : 인덱스 필드는 쓰이지 않고, RGB 값의 최상위 바이트가 색 지정 방식이다.
            // 0xC3이면 하위 바이트가 색상 인덱스이다. (R2004 파일로 확인, 그 밖의 방식은 확인하지 않음)
            int method = (colorData[1] >>> 24) & 0xFF;
            if (method == 0xC3) {
                colorIndex = colorData[1] & 0xFF;
            }
        }
        layer.setColor(new CmColor(colorIndex));
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayer layer = (DwgLayer) target;
        long self = target.handle();
        r.readHandleReference(self);                // xref block (null)
        if (v.from(DwgVersion.R2000)) {
            layer.setPlotStyleHandle(new DwgHandleRef(r.readHandleReference(self)));
        }
        if (v.from(DwgVersion.R2007)) {
            r.readHandleReference(self);            // material
        }
        layer.setLineTypeHandle(new DwgHandleRef(r.readHandleReference(self)));
    }
}
