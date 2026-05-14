package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * ACAD_PROXY_ENTITY - 커스텀 프록시 엔티티 (R13+)
 * 자동화 애플리케이션이 추가한 커스텀 엔티티
 */
public class DwgProxyEntity extends AbstractDwgEntity {
    private int proxyVersion;
    private int classVersion;
    private int appInfoSize;
    private byte[] appInfo;
    private int objectDataSize;
    private byte[] objectData;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_PROXY_ENTITY; }

    public int proxyVersion() { return proxyVersion; }
    public int classVersion() { return classVersion; }
    public int appInfoSize() { return appInfoSize; }
    public byte[] appInfo() { return appInfo; }
    public int objectDataSize() { return objectDataSize; }
    public byte[] objectData() { return objectData; }

    public void setProxyVersion(int proxyVersion) { this.proxyVersion = proxyVersion; }
    public void setClassVersion(int classVersion) { this.classVersion = classVersion; }
    public void setAppInfoSize(int appInfoSize) { this.appInfoSize = appInfoSize; }
    public void setAppInfo(byte[] appInfo) { this.appInfo = appInfo; }
    public void setObjectDataSize(int objectDataSize) { this.objectDataSize = objectDataSize; }
    public void setObjectData(byte[] objectData) { this.objectData = objectData; }
}
