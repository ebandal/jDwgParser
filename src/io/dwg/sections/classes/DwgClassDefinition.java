package io.dwg.sections.classes;

/**
 * 커스텀 클래스 정의 (스펙 10장). 객체 타입 번호 500 이상은 이 정의의 클래스 번호이다.
 * {@link #version()}은 R14부터 프록시 플래그이다.
 */
public class DwgClassDefinition {
    private int classNumber;
    private int version;
    private String applicationName;
    private String cppClassName;
    private String dxfRecordName;
    private boolean wasAZombie;
    private boolean isAnEntity;
    private int instanceCount;
    private int dwgVersion;
    private int maintenanceVersion;

    public int classNumber() { return classNumber; }
    public int version() { return version; }
    public String applicationName() { return applicationName; }
    public String cppClassName() { return cppClassName; }
    public String dxfRecordName() { return dxfRecordName; }
    public boolean wasAZombie() { return wasAZombie; }
    public boolean isEntity() { return isAnEntity; }
    /** R2004+: the number of objects of this class in the drawing (DXF 91). */
    public int instanceCount() { return instanceCount; }
    /** R2004+ */
    public int dwgVersion() { return dwgVersion; }
    /** R2004+ */
    public int maintenanceVersion() { return maintenanceVersion; }

    public void setClassNumber(int classNumber) { this.classNumber = classNumber; }
    public void setVersion(int version) { this.version = version; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public void setCppClassName(String cppClassName) { this.cppClassName = cppClassName; }
    public void setDxfRecordName(String dxfRecordName) { this.dxfRecordName = dxfRecordName; }
    public void setWasAZombie(boolean wasAZombie) { this.wasAZombie = wasAZombie; }
    public void setAnEntity(boolean anEntity) { this.isAnEntity = anEntity; }
    public void setInstanceCount(int count) { this.instanceCount = count; }
    public void setDwgVersion(int version) { this.dwgVersion = version; }
    public void setMaintenanceVersion(int version) { this.maintenanceVersion = version; }
}
