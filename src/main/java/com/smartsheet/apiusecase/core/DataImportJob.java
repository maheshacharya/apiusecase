package com.smartsheet.apiusecase.core;

import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.models.Row;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataImportJob implements Job {

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {

        try {
            SystemContextManager manager = (SystemContextManager) jec.getScheduler().getContext().get("contextManager");
            SmartsheetConfig config = (SmartsheetConfig) jec.getScheduler().getContext().get("smartsheetConfig");
            SheetHandler handler = (SheetHandler) jec.getScheduler().getContext().get("sheetHandler");
            runDataImportJob(manager, config, handler);
        } catch (SchedulerException e) {
            e.printStackTrace();
        } catch (SmartsheetException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    /**
     * @param ctxManager
     * @param config
     * @param sheetHandler
     * @throws com.smartsheet.api.SmartsheetException
     */
    public static void runDataImportJob(SystemContextManager ctxManager,
                                        SmartsheetConfig config,
                                        SheetHandler sheetHandler)
            throws SmartsheetException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Map<String, Object> logMap = new HashMap();
        try {
            sheetHandler.setCtxManager(ctxManager);
            sheetHandler.handleSheet();
            //update config sheet logs
            ctxManager.setLastExecutionTime(new Date());
            logMap.put("Message", "SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            logMap.put("Total Records Processed", 0);
            logMap.put("Message", "ERROR : " + e.getMessage());
        } finally {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = ctxManager.getLastExecutionTime();
            if (date == null) {
                date = new Date();
            }
            String value = dateFormat.format(date);
            logMap.put("Last Execution Time", value);
            logMap.put("Total Records Processed", sheetHandler.getTotalResultsCount());
            List<Row> updatedRows = ctxManager.getConnector().
                    getSmartsheet()
                    .sheetResources()
                    .rowResources()
                    .updateRows(config.getConfigSheetId(), config.buildUpdateConfigSheetRow(logMap));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


}
