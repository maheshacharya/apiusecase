package com.smartsheet.apiusecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.models.Row;
import com.smartsheet.apiusecase.core.*;
import com.smartsheet.apiusecase.model.config.ConfigInfo;
import com.smartsheet.apiusecase.model.config.ConfigItem;
import com.smartsheet.apiusecase.util.ConfigUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootApplication
public class ApiusecaseApplication implements CommandLineRunner, ConfigListener {

    private static final Logger logger = LoggerFactory.getLogger(ApiusecaseApplication.class);


    @Autowired
    private SmartsheetConfig config;


    @Autowired
    private SystemContextManager ctxManager;




    private ConfigInfo configInfo = null;
    private List<Scheduler> schedulers;


    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ApiusecaseApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);


    }


    /**
     * @throws org.quartz.SchedulerException
     */
    private void scheduleConfigSynJob() throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();

        JobDetail job = JobBuilder.newJob(SmartsheetConfigSyncjob.class)
                .withIdentity("smartsheetConfigJob", "apisueconfig").build();


        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("smartsheetConfigTrigger", "apiusecofig")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))
                .startNow()
                .build();
        scheduler.getContext().put("contextManager", ctxManager);
        scheduler.getContext().put("smartsheetConfig", config);
        new Thread() {
            @Override
            public void run() {
                try {
                    scheduler.start();
                    scheduler.scheduleJob(job, trigger);
                } catch (SchedulerException e) {
                    //log
                }
            }
        }.start();

    }


    /**
     * @param jobName
     * @param scheduleGroupName
     * @param handler
     * @throws SchedulerException
     * @throws com.smartsheet.api.SmartsheetException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    private void scheduleJob(String jobName, String scheduleGroupName, SheetHandler handler) throws SchedulerException, SmartsheetException, UnsupportedEncodingException, NoSuchAlgorithmException {


        schedulers = new ArrayList<>();
        new Thread() {
            @Override
            public void run() {
                try {

                    List<Trigger> triggers = getTriggers(scheduleGroupName);
                    int index = 0;
                    for (Trigger trigger : triggers) {
                        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
                        scheduler.getContext().put("contextManager", ctxManager);
                        scheduler.getContext().put("smartsheetConfig", config);
                        scheduler.getContext().put("sheetHandler", handler);
                        JobDetail job = JobBuilder.newJob(DataImportJob.class)
                                .withIdentity(jobName + index, "config").build();
                        scheduler.scheduleJob(job, trigger);
                        logger.info("starting  the scheduler " + scheduler.getSchedulerName());
                        scheduler.start();
                        schedulers.add(scheduler);
                        index++;
                    }

                } catch (SchedulerException e) {
                    logger.warn("Scheduler Exception ", e);
                } catch (SmartsheetException e) {
                    logger.warn("Smartsheet Exception", e);
                } catch (NoSuchAlgorithmException e) {
                    logger.warn("NoSuchAlgorithmException", e);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("UnsupportedEncodingException", e);
                } catch (JsonProcessingException e) {
                    logger.warn("Exception", e);
                }
            }
        }.start();
    }


    /**
     * @param scheduleGroupName
     * @return
     * @throws SmartsheetException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public List<Trigger> getTriggers(String scheduleGroupName) throws SmartsheetException, UnsupportedEncodingException, NoSuchAlgorithmException, JsonProcessingException {

        List<Trigger> triggers = new ArrayList<>();
        ConfigItem items = ConfigUtil.findConfigItemByName(config.buildConfigInfo(), scheduleGroupName);
        for (ConfigItem item : items.getItems()) {
            Trigger trigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(item.getName(), "apiuse")
                    .withSchedule(CronScheduleBuilder.cronSchedule(item.getValue()))
                    .build();
            triggers.add(trigger);
        }

        return triggers;
    }

    private  ExcelImportDataHandler handler;

    @Override
    public void run(String... args) throws Exception {

        config.addListener(this);
        configInfo = config.buildConfigInfo();

        ctxManager.setConfigInfo(configInfo);
        ctxManager.setConnector(config.getSmartsheetConnector());
        //Config sheet sync scheduler.
      //  scheduleConfigSynJob();

         handler = new ExcelImportDataHandler(ctxManager);




        Map<String, Object> logMap = new HashMap();
        try {
            //run handler one time
            handler.handleSheet();
            //update config sheet logs
            ctxManager.setLastExecutionTime(new Date());
            logMap.put("Message", "SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            logMap.put("Message", "ERROR : " + e.getMessage());
        } finally {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = ctxManager.getLastExecutionTime();
            if (date == null) {
                date = new Date();
            }
            String value = dateFormat.format(date);
            logMap.put("Last Execution Date", value);
            List<Row> updatedRows = ctxManager.getConnector().
                    getSmartsheet()
                    .sheetResources()
                    .rowResources()
                    .updateRows(config.getConfigSheetId(), config.buildUpdateConfigSheetRow(logMap));


        }


       // scheduleJob("DataImport", "Schedules", handler);



    }


    @Override
    public synchronized void configChanged() {

        logger.info("Config Changed");

        //shut down all schedulers
        for (Scheduler scheduler : schedulers) {
            try {
                logger.info("shutting down the scheduler " + scheduler.getSchedulerName());
                scheduler.shutdown();
            } catch (SchedulerException e) {
                logger.warn("Scheduler Exception ", e);
            }
        }

        //re schedule everything...
        try {
            scheduleJob("DataImport", "Schedules", handler);

        } catch (SchedulerException e) {
            logger.warn("Scheduler Exception ", e);
        } catch (SmartsheetException e) {
            logger.warn("Smartsheet Exception", e);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("NoSuchAlgorithmException", e);
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
        }
    }
}
