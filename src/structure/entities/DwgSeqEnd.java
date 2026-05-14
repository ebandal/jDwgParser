package structure.entities;

import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * SEQEND 엔티티 (타입 0x04)
 * POLYLINE/SHAPE의 끝을 표시하는 마커
 */
public class DwgSeqEnd extends AbstractDwgEntity {
    @Override
    public DwgObjectType objectType() { return DwgObjectType.SEQEND; }
}
