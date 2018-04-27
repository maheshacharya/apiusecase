package com.smartsheet.apiusecase.util;




import com.smartsheet.api.models.Cell;
import com.smartsheet.api.models.Column;
import com.smartsheet.api.models.Row;
import com.smartsheet.api.models.Sheet;
import com.smartsheet.apiusecase.model.config.ConfigInfo;
import com.smartsheet.apiusecase.model.config.ConfigItem;

import java.util.HashMap;
import java.util.Map;


public class ConfigUtil {


    /**
     * @param info
     * @param parentName
     * @param childName
     * @return
     */
    public static ConfigItem getConfigItem(ConfigInfo info, String parentName, String childName) {
        for (ConfigItem item : info) {
            if (item.getName() == null) {
                continue;
            }
            if (item.getName().equals(parentName)) {
                for (ConfigItem cItem : item.getItems()) {
                    if (cItem.getName().equals(childName)) {
                        return cItem;
                    }
                }
            }
        }

        return null;

    }

    /**
     * @param info
     * @param parentName
     * @param childName
     * @return
     */
    public static String getConfigItemValue(ConfigInfo info, String parentName, String childName) {
        ConfigItem item = getConfigItem(info, parentName, childName);
        if (item != null) {
            return item.getValue();
        }
        return null;

    }

    /**
     * @param item
     * @param name
     * @return
     */
    public static String getChildConfigItemValue(ConfigItem item, String name) {
        for (ConfigItem citem : item.getItems()) {
            if (citem.getName().equalsIgnoreCase(name)) {
                return citem.getValue();
            }
        }
        return null;

    }


    /**
     * @param info
     * @param name
     * @return
     */
    public static ConfigItem findConfigItemByName(ConfigInfo info, String name) {

        for (ConfigItem item : info) {
            if(item.getName()==null){
                continue;
            }
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }


    /**
     * @param sheet
     * @param columnName
     * @param cellValue
     * @return
     */
    public static Long getRowId(Sheet sheet, String columnName, String cellValue) {

        Long columnId = null;
        for (Column col : sheet.getColumns()) {
            if (col.getTitle().equals(columnName)) {
                columnId = col.getId();
                break;
            }
        }

        for (Row row : sheet.getRows()) {
            for (Cell cell : row.getCells()) {
                if (cell.getColumnId().equals(columnId)) {
                    if (cell.getDisplayValue() == null) {
                        continue;
                    }
                    if (cell.getDisplayValue().equals(cellValue)) {
                        return row.getId();
                    }
                }
            }

        }
        return null;

    }

    /**
     * @param sheet
     * @param title
     * @return
     */
    public static Long getColumnId(Sheet sheet, String title) {

        Long columnId = null;
        for (Column col : sheet.getColumns()) {
            if (col.getTitle().equals(title)) {
                columnId = col.getId();
                break;
            }
        }

        return columnId;
    }

    /**
     *
     * @param sheet
     * @return
     */
    public static Map<String,Long> buildColumnIdMap(Sheet sheet){
        Map<String,Long> map = new HashMap();
        for(Column col:sheet.getColumns()){
            map.put(col.getTitle(), col.getId());
        }
        return map;
    }


    /**
     *
     * @param sheet
     * @return
     */
    public static Map<String,Integer> buildColumnIndexMap(Sheet sheet){
        Map<String,Integer> map = new HashMap();
        for(Column col:sheet.getColumns()){
            map.put(col.getTitle(), col.getIndex());
        }
        return map;
    }


    public static Long getSheetIdForHyperLink(ConfigItem item){
        return  getSheetIdForHyperLink(item, 1);
    }

    public static Long getSheetIdForHyperLink(ConfigItem item, int valueColumnIndex){
        Row row = (Row) item.getDataSource();
        Cell cell = row.getCells().get(valueColumnIndex);
        return cell.getHyperlink().getSheetId();
    }


    public static String getHash256(String text) {
        try {
            return org.apache.commons.codec.digest.DigestUtils.sha256Hex(text);
        } catch (Exception ex) {

            return "";
        }
    }

}
