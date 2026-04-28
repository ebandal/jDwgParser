package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgDataSource extends AbstractDwgEntity {
    private String dataSourceName;
    private String sourceType;
    private String connectionString;
    private String sourceFile;
    private int refreshInterval;
    private boolean isAutoRefresh;
    private String lastRefreshTime;
    private int dataSourceState;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_DATASOURCE;
    }

    public String dataSourceName() { return dataSourceName; }
    public void setDataSourceName(String dataSourceName) { this.dataSourceName = dataSourceName; }

    public String sourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String connectionString() { return connectionString; }
    public void setConnectionString(String connectionString) { this.connectionString = connectionString; }

    public String sourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public int refreshInterval() { return refreshInterval; }
    public void setRefreshInterval(int refreshInterval) { this.refreshInterval = refreshInterval; }

    public boolean isAutoRefresh() { return isAutoRefresh; }
    public void setAutoRefresh(boolean autoRefresh) { isAutoRefresh = autoRefresh; }

    public String lastRefreshTime() { return lastRefreshTime; }
    public void setLastRefreshTime(String lastRefreshTime) { this.lastRefreshTime = lastRefreshTime; }

    public int dataSourceState() { return dataSourceState; }
    public void setDataSourceState(int dataSourceState) { this.dataSourceState = dataSourceState; }
}
