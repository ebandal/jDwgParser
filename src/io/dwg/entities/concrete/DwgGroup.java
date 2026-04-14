package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

/**
 * GROUP 엔티티 (타입 0x3C)
 * 여러 엔티티를 그룹화 (이름이 있는 그룹)
 */
public class DwgGroup extends AbstractDwgEntity {
    private String groupName;        // 그룹 이름
    private boolean isUnnamed;       // 이름 없는 그룹 여부
    private boolean isSelectable;    // 선택 가능 여부
    private List<Object> members;    // 그룹 멤버 (핸들 리스트)

    public DwgGroup() {
        this.members = new ArrayList<>();
    }

    @Override
    public DwgObjectType objectType() { return DwgObjectType.GROUP; }

    public String groupName() { return groupName; }
    public boolean isUnnamed() { return isUnnamed; }
    public boolean isSelectable() { return isSelectable; }
    public List<Object> members() { return members; }

    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setUnnamed(boolean unnamed) { this.isUnnamed = unnamed; }
    public void setSelectable(boolean selectable) { this.isSelectable = selectable; }
    public void addMember(Object member) { this.members.add(member); }
}
