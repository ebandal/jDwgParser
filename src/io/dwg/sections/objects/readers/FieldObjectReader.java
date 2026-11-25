package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgField;
import io.dwg.sections.objects.ObjectReader;

/**
 * FIELD (a class), spec §20.4.76, kept in the object's values:
 * <pre>
 *   EVALUATOR TV, CODE TV, child field count BL, field object count BL, R13–R2004 FORMAT TV,
 *   EVALUATIONOPTIONS, FILINGOPTIONS, STATE, STATUS, ERRORCODE BL, ERRORMESSAGE TV, VALUE (a value,
 *   §20.4.99), VALUETEXT TV, VALUETEXTLENGTH BL, child value count BL and per child a key TV and a
 *   value (CHILDVALUES, a map)
 *   handles: (parent, reactors, xdictionary,) the child fields, the field objects, then one per
 *   value of type object id
 * </pre>
 * The specification gives the value text length (DXF 98) as TV; it is a BL (read so, every FIELD
 * of the samples ends its data and its handles exactly).
 */
public class FieldObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_FIELD.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgField field = (DwgField) target;
        Fields.read(field, r, "EVALUATOR TV", "CODE TV", "CHILDCOUNT BL", "OBJECTCOUNT BL");
        if (!v.from(DwgVersion.R2007)) {
            Fields.read(field, r, "FORMAT TV");
        }
        Fields.read(field, r, "EVALUATIONOPTIONS BL", "FILINGOPTIONS BL", "STATE BL", "STATUS BL",
                "ERRORCODE BL", "ERRORMESSAGE TV");
        java.util.Map<String, Object> value = DwgValueReader.read(r, v);
        field.setValue("VALUE", value);
        Fields.read(field, r, "VALUETEXT TV", "VALUETEXTLENGTH BL");
        int children = r.readBitLong();
        java.util.Map<String, Object> childValues = new java.util.LinkedHashMap<>();
        for (int i = 0; i < children; i++) {
            String key = r.readVariableText();
            childValues.put(key, DwgValueReader.read(r, v));
        }
        field.setValue("CHILDVALUES", childValues);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgField field = (DwgField) target;
        long self = target.handle();
        for (int i = 0; i < (Integer) field.value("CHILDCOUNT"); i++) {
            field.addChildFieldHandle(new DwgHandleRef(r.readHandleReference(self)));
        }
        for (int i = 0; i < (Integer) field.value("OBJECTCOUNT"); i++) {
            field.addObjectHandle(new DwgHandleRef(r.readHandleReference(self)));
        }
    }
}
