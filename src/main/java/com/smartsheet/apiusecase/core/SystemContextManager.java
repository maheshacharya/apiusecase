package com.smartsheet.apiusecase.core;

import com.smartsheet.apiusecase.model.config.ConfigInfo;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SystemContextManager {

    private SmartSheetConnector connector;
    private ConfigInfo configInfo;
    private Date lastExecutionTime;


    public SmartSheetConnector getConnector() {
        return connector;
    }

    public void setConnector(SmartSheetConnector connector) {
        this.connector = connector;
    }

    public ConfigInfo getConfigInfo() {
        return configInfo;
    }

    public void setConfigInfo(ConfigInfo configInfo) {
        this.configInfo = configInfo;
    }

    public Date getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(Date lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }
}
