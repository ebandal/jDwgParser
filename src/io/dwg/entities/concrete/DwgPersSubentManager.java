package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

public class DwgPersSubentManager extends AbstractDwgEntity {
    private String managerName;
    private int subentityCount;
    private List<Integer> subentityIds;
    private List<String> subentityNames;
    private int managerFlags;
    private boolean isActive;

    public DwgPersSubentManager() {
        this.subentityIds = new ArrayList<>();
        this.subentityNames = new ArrayList<>();
    }

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_PERSSUBENTMANAGER;
    }

    public String managerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public int subentityCount() { return subentityCount; }
    public void setSubentityCount(int subentityCount) { this.subentityCount = subentityCount; }

    public List<Integer> subentityIds() { return subentityIds; }

    public List<String> subentityNames() { return subentityNames; }

    public int managerFlags() { return managerFlags; }
    public void setManagerFlags(int managerFlags) { this.managerFlags = managerFlags; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
