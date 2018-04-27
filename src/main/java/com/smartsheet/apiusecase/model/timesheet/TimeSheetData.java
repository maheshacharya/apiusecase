package com.smartsheet.apiusecase.model.timesheet;

import java.util.ArrayList;
import java.util.List;

public class TimeSheetData {

    private List<ProjectInfo> projectInfoList = new ArrayList();


    public List<ProjectInfo> getProjectInfoList() {
        return projectInfoList;
    }

    public void setProjectInfoList(List<ProjectInfo> projectInfoList) {
        this.projectInfoList = projectInfoList;
    }
}
