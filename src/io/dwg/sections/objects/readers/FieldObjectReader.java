package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgField;
import io.dwg.sections.objects.ObjectReader;

public class FieldObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_FIELD.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgField field = (DwgField) target;

        // Field expression
        field.setFieldExpression(r.readText());

        // Field value
        field.setFieldValue(r.readText());

        // Evaluation status
        field.setEvaluationStatus(r.readBitShort());

        // Field format
        field.setFieldFormat(r.readText());

        // Field locked status
        field.setFieldLocked(r.readBitShort());
    }
}
