package com.smartsheet.apiusecase.model.timesheet;

public class ProjectInfo {
    private String projectCode;
    private int hours;

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }
}
