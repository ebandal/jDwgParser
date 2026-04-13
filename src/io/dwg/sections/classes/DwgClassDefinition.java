package io.dwg.sections.classes;

/**
 * 커스텀 클래스 정의. 프록시 엔티티 처리에 필요.
 */
public class DwgClassDefinition {
    private int classNumber;
    private int version;
    private String applicationName;
    private String cppClassName;
    private String dxfRecordName;
    private boolean wasAZombie;
    private boolean isAnEntity;

    public int classNumber() { return classNumber; }
    public int version() { return version; }
    public String applicationName() { return applicationName; }
    public String cppClassName() { return cppClassName; }
    public String dxfRecordName() { return dxfRecordName; }
    public boolean wasAZombie() { return wasAZombie; }
    public boolean isEntity() { return isAnEntity; }

    public void setClassNumber(int classNumber) { this.classNumber = classNumber; }
    public void setVersion(int version) { this.version = version; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public void setCppClassName(String cppClassName) { this.cppClassName = cppClassName; }
    public void setDxfRecordName(String dxfRecordName) { this.dxfRecordName = dxfRecordName; }
    public void setWasAZombie(boolean wasAZombie) { this.wasAZombie = wasAZombie; }
    public void setAnEntity(boolean anEntity) { this.isAnEntity = anEntity; }
}
