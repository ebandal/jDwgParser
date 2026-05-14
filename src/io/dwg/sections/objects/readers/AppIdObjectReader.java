package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAppId;
import io.dwg.sections.objects.ObjectReader;

/**
 * APPID (애플리케이션 식별자) 테이블 엔트리 ObjectReader.
 * 타입 0x39
 */
public class AppIdObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.APPID.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgAppId appId = (DwgAppId) target;

        appId.setName(r.readVariableText());
        appId.setFlags(r.readBitShort());
    }
}
