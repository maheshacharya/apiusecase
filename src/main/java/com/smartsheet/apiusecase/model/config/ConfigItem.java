package com.smartsheet.apiusecase.model.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigItem {

    private String name;
    private String value;
    private List<ConfigItem> items = new ArrayList();
    private Object dataSource;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ConfigItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigItem> items) {
        this.items = items;
    }

    public Object getDataSource() {
        return dataSource;
    }

    public void setDataSource(Object dataSource) {
        this.dataSource = dataSource;
    }
}
