package com.smartsheet.apiusecase.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartsheet.api.SmartsheetException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class SmartsheetConfigSyncjob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(SmartsheetConfigSyncjob.class);

    /**
     *
     * @param jec
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        try {
            logger.info("Reload Smartsheet configurations");
            SystemContextManager manager = (SystemContextManager) jec.getScheduler().getContext().get("contextManager");
            SmartsheetConfig config = (SmartsheetConfig) jec.getScheduler().getContext().get("smartsheetConfig");
            manager.setConfigInfo(config.buildConfigInfo());
            logger.info("Smartsheet configurations loaded");
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
}
