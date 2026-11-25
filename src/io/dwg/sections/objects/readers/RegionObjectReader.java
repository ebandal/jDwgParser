package io.dwg.sections.objects.readers;

import io.dwg.entities.DwgObjectType;

/**
 * REGION, spec §20.4.41; see {@link AcisReader}. The specification gives every R2007+ ACIS entity
 * a history handle; the R2007+ REGIONs of the samples have none (read with it, their handles run
 * past the end of the object; without it they end exactly).
 */
public class RegionObjectReader extends AcisReader {

    @Override
    public int objectType() { return DwgObjectType.REGION.typeCode(); }

    @Override
    protected boolean hasHistoryHandle() { return false; }
}
