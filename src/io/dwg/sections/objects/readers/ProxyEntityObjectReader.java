package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgProxyEntity;
import io.dwg.sections.objects.ObjectReader;

public class ProxyEntityObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_PROXY_ENTITY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgProxyEntity proxy = (DwgProxyEntity) target;

        // Proxy version
        proxy.setProxyVersion(r.readBitShort());

        // Class version
        proxy.setClassVersion(r.readBitShort());

        // Application info
        int appInfoSize = r.readBitShort();
        proxy.setAppInfoSize(appInfoSize);
        if (appInfoSize > 0) {
            byte[] appInfo = new byte[appInfoSize];
            // Read raw bytes (simplified)
            proxy.setAppInfo(appInfo);
        }

        // Object data
        int objectDataSize = r.readBitShort();
        proxy.setObjectDataSize(objectDataSize);
        if (objectDataSize > 0) {
            byte[] objectData = new byte[objectDataSize];
            // Read raw bytes (simplified)
            proxy.setObjectData(objectData);
        }
    }
}
