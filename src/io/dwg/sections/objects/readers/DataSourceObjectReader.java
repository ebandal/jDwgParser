package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDataSource;
import io.dwg.sections.objects.ObjectReader;

public class DataSourceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_DATASOURCE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDataSource source = (DwgDataSource) target;

        // Data source name
        source.setDataSourceName(r.readText());

        // Source type
        source.setSourceType(r.readText());

        // Connection string
        source.setConnectionString(r.readText());

        // Source file path
        source.setSourceFile(r.readText());

        // Refresh interval
        source.setRefreshInterval(r.readBitShort());

        // Is auto-refresh enabled
        source.setAutoRefresh(r.readBitShort() != 0);

        // Last refresh time
        source.setLastRefreshTime(r.readText());

        // Data source state
        source.setDataSourceState(r.readBitShort());
    }
}
