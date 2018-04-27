package com.smartsheet.apiusecase.core;


import com.smartsheet.api.SmartsheetException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;

@Service
public class SheetHandler {


    protected SystemContextManager ctxManager;

    private int totalResultsCount = 0;

    /**
     * @throws com.smartsheet.api.SmartsheetException
     * @throws java.io.IOException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws IllegalAccessException
     */
    public void handleSheet() throws SmartsheetException, IOException, InvocationTargetException, IllegalAccessException, URISyntaxException, ParseException {


    }





    public SystemContextManager getCtxManager() {
        return ctxManager;
    }

    public void setCtxManager(SystemContextManager ctxManager) {
        this.ctxManager = ctxManager;
    }

    public int getTotalResultsCount() {
        return totalResultsCount;
    }


}
