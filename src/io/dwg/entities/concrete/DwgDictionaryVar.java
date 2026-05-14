package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgDictionaryVar extends AbstractDwgEntity {
    private String varName;
    private String varValue;
    private int varType;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_DICTIONARYVAR;
    }

    public String varName() { return varName; }
    public void setVarName(String varName) { this.varName = varName; }

    public String varValue() { return varValue; }
    public void setVarValue(String varValue) { this.varValue = varValue; }

    public int varType() { return varType; }
    public void setVarType(int varType) { this.varType = varType; }
}
