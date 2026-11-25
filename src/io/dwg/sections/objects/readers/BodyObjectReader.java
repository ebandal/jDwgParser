package io.dwg.sections.objects.readers;

import io.dwg.entities.DwgObjectType;

/** BODY, spec §20.4.41; see {@link AcisReader}. */
public class BodyObjectReader extends AcisReader {

    @Override
    public int objectType() { return DwgObjectType.BODY.typeCode(); }
}
