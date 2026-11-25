package io.dwg.sections.objects.readers;

import io.dwg.entities.DwgObjectType;

/** SOLID3D, spec §20.4.41; see {@link AcisReader}. */
public class Solid3dObjectReader extends AcisReader {

    @Override
    public int objectType() { return DwgObjectType.SOLID3D.typeCode(); }
}
