package com.smartsheet.apiusecase.core;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.models.Cell;
import com.smartsheet.api.models.Row;
import com.smartsheet.api.models.Sheet;
import com.smartsheet.apiusecase.model.config.ConfigInfo;
import com.smartsheet.apiusecase.model.config.ConfigItem;
import com.smartsheet.apiusecase.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@PropertySource("file:smartsheet.properties")
public class SmartsheetConfig {

    private static final Logger logger = LoggerFactory.getLogger(SmartsheetConfig.class);
    @Value("${smartsheet.config.sheet.id}")
    private String sheetId;

    @Value("${smartsheet.access.token}")
    private String accessToken;


    private Map<String, String> schedulesHash = new HashMap<>();

    private List<ConfigListener> listeners = new ArrayList<ConfigListener>();

    public void addListener(ConfigListener listener) {
        listeners.add(listener);
    }

    public Long getConfigSheetId() {

        return Long.parseLong(sheetId);
    }


    public SmartSheetConnector getSmartsheetConnector() {
        SmartSheetConnector connector = new SmartSheetConnector(accessToken);
        return connector;
    }


    /**
     * @param logMap
     * @return
     * @throws com.smartsheet.api.SmartsheetException
     */
    public List<Row> buildUpdateConfigSheetRow(Map<String, Object> logMap) throws SmartsheetException {
        Sheet configSheet = getConfigSheet();
        List<Row> updateRows = new ArrayList<>();
        Long colId = ConfigUtil.getColumnId(configSheet, "Value");
        for (Object key : logMap.keySet()) {
            Long rowId = ConfigUtil.getRowId(configSheet, "Name", key.toString());
            Cell.UpdateRowCellsBuilder builder = new Cell.UpdateRowCellsBuilder();
            List<Cell> cells = builder.addCell(colId, logMap.get(key)).build();
            Row row = new Row.UpdateRowBuilder().setRowId(rowId).setCells(cells).build();
            updateRows.add(row);
        }
        return updateRows;


    }


    private Sheet getConfigSheet() throws SmartsheetException {
        SmartSheetConnector connector = getSmartsheetConnector();
        return connector.getSheet(getConfigSheetId());
    }

    /**
     * @return
     * @throws com.smartsheet.api.SmartsheetException
     */
    public ConfigInfo buildConfigInfo() throws SmartsheetException, UnsupportedEncodingException, NoSuchAlgorithmException, JsonProcessingException {

        Sheet configSheet = getConfigSheet();
        if (configSheet == null) {
            new SmartsheetException("Sheet for id: " + sheetId + " doesn't exist");
        }


        ConfigInfo configInfo = new ConfigInfo();


        Row parentRow = null;
        ConfigItem item = null;
        for (Row row : configSheet.getRows()) {

            if (row.getParentId() == null) {
                parentRow = row;
                item = new ConfigItem();
                item.setName(row.getCells().get(0).getDisplayValue());
                configInfo.add(item);
            }

            if (parentRow != null && row.getParentId() != null) {

                if (row.getParentId().equals(parentRow.getId())) {

                    ConfigItem childItem = new ConfigItem();
                    childItem.setDataSource(row);
                    item.getItems().add(childItem);

                    childItem.setName(row.getCells().get(0).getDisplayValue());
                    String value = "";
                    if (row.getCells().get(1).getLinkInFromCell() != null) {
                        value = "" + row.getCells().get(1).getLinkInFromCell().getSheetId();
                    } else if (row.getCells().get(1).getHyperlink() != null) {
                        value = "" + row.getCells().get(1).getHyperlink().getSheetId();


                    } else {
                        value = row.getCells().get(1).getDisplayValue();
                    }
                    childItem.setValue(value);


                }
            }

        }

        schedulesChangeCheck(configInfo, "Schedules");
        return configInfo;

    }


    /**
     * @param configInfo
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    private void schedulesChangeCheck(ConfigInfo configInfo, String group) throws UnsupportedEncodingException, NoSuchAlgorithmException, JsonProcessingException {

        ConfigItem item = ConfigUtil.findConfigItemByName(configInfo, group);
        Map<String, String> valueMap = new HashMap<>();
        for(ConfigItem it:item.getItems()){
            valueMap.put(it.getName(), it.getValue());
        }
        ObjectMapper mapper = new ObjectMapper();
        String value = mapper.writeValueAsString(valueMap);

        String newHash = getDigestForString(value);if (schedulesHash.get(group) == null) {
            schedulesHash.put(group, newHash);
        } else {
            //if the schedules changed then fire the event
            if (!schedulesHash.get(group).equals(newHash)) {
                for (ConfigListener l : listeners) {
                    l.configChanged();
                    logger.info("Scheduler Configuration Changed.");
                }

                schedulesHash.put(group, newHash);
            }

        }

    }

    /**
     * @param value
     * @return
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    private String getDigestForString(String value) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = value.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(bytesOfMessage);
        return new String(Base64.getEncoder().encode(thedigest));
    }


    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
