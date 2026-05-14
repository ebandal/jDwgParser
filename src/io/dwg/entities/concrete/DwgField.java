package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * ACAD_FIELD 엔티티 - 계산 필드 (R2005+)
 */
public class DwgField extends AbstractDwgEntity {
    private String fieldExpression;
    private String fieldValue;
    private int evaluationStatus;  // 0=Not evaluated, 1=Evaluated, 2=Error
    private String fieldFormat;
    private int fieldLocked;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_FIELD; }

    public String fieldExpression() { return fieldExpression; }
    public String fieldValue() { return fieldValue; }
    public int evaluationStatus() { return evaluationStatus; }
    public String fieldFormat() { return fieldFormat; }
    public int fieldLocked() { return fieldLocked; }

    public void setFieldExpression(String fieldExpression) { this.fieldExpression = fieldExpression; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
    public void setEvaluationStatus(int evaluationStatus) { this.evaluationStatus = evaluationStatus; }
    public void setFieldFormat(String fieldFormat) { this.fieldFormat = fieldFormat; }
    public void setFieldLocked(int fieldLocked) { this.fieldLocked = fieldLocked; }
}
