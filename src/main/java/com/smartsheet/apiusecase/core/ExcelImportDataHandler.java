package com.smartsheet.apiusecase.core;


import com.smartsheet.api.Smartsheet;
import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.models.*;
import com.smartsheet.apiusecase.model.config.ConfigItem;
import com.smartsheet.apiusecase.util.ConfigSheetConstantNames;
import com.smartsheet.apiusecase.util.ConfigUtil;
import com.smartsheet.apiusecase.util.ExcelImportExportItemNames;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ExcelImportDataHandler extends SheetHandler {

    private SystemContextManager ctxManager;
    private Map<String, Long> customerProjectSheetColumnMap = new HashMap();
    private static final Logger logger = LoggerFactory.getLogger(ExcelImportDataHandler.class);


    public ExcelImportDataHandler(SystemContextManager ctxManager) {
        this.ctxManager = ctxManager;

    }

    /**
     * @param rows
     * @param projectNumber
     * @return
     */
    private Long getParentIdforProjectNumber(List<Row> rows, String projectNumber) {
        for (Row r : rows) {
            for (Cell cell : r.getCells()) {
                if (cell.getColumnId().equals(customerProjectSheetColumnMap.get("Project Number"))) {
                    if (cell.getDisplayValue().equals(projectNumber)) {
                        return r.getId();
                    }
                }


            }
        }

        return null;
    }


    public void handleSheet() throws SmartsheetException, IOException {

        File file = createLocalCopyOfLatestAtachment();
        TreeMap<String, List<Object>> dataMap = buildDataMapFromFileData(file);
        Map<String, List<Object>> projectDataMap = buildProjectDataMap(dataMap);
        Long sheetId = getCustomerProjectSheetId();

        /**
         *  Check for new project id,
         *  add the new project to the bottom of the sheet,
         *  which will become parent for new project items with matching id.
         */

        Map<String, List<Row>> customerProjectList = getCustomerProjectList();
        List<Row> newRows = new ArrayList();
        List<Row> updateRows = new ArrayList();
        List<Row> parentRows = customerProjectList.get("parentRows");


        for (Object key : projectDataMap.keySet()) {
            if (key.toString().equals("Project")) {
                //this is the heading section in the excel data sheet
                continue;
            }

            //if the key is not contained in the sheet data,
            // create a new "Project Number" parent item
            if (!customerProjectList.containsKey(key)) {
                logger.info("Adding new project \"{}\" to the bottom", key);
                List<Cell> cells = new Cell.AddRowCellsBuilder().addCell(customerProjectSheetColumnMap.get("Project Number"), key, false, null, null).build();
                newRows.add(new Row.AddRowBuilder().setCells(cells).setToBottom(true).build());
            }
        }
        if (!newRows.isEmpty()) {
            //add new rows (parent)
            List<Row> newRowsX = ctxManager.getConnector().getSmartsheet().sheetResources().rowResources().addRows(
                    sheetId,    // sheet Id
                    newRows);
        }
        addOrUpdateWIP();

    }


    public void addOrUpdateWIP() throws SmartsheetException, IOException {

        File file = createLocalCopyOfLatestAtachment();
        TreeMap<String, List<Object>> dataMap = buildDataMapFromFileData(file);
        Map<String, List<Object>> projectDataMap = buildProjectDataMap(dataMap);

        /**
         *  Check for new project id,
         *  add the new project to the bottom of the sheet,
         *  which will become parent for new project items with matching id.
         */

        Map<String, List<Row>> customerProjectList = getCustomerProjectList();

        List<Row> newRows = new ArrayList();

        List<Row> updateRows = new ArrayList();

        List<Row> parentRows = customerProjectList.get("parentRows");


        for (Object key : projectDataMap.keySet()) {
            if (key.toString().equals("Project")) {
                //this is the heading section in the excel data sheet
                continue;
            }

            //Check for WIP already exists.
            List<Object> projectItems = projectDataMap.get(key);
            List<Row> rows = customerProjectList.get(key);
            boolean foundMatchingWIP = false;
            for (Object item : projectItems) {
                Object[] cellValues = (Object[]) item;
                String wip = cellValues[1].toString();
                //do we have a row in the sheet
                for (Row row : rows) {
                    for (Cell cell : row.getCells()) {
                        if (cell.getColumnId().equals(customerProjectSheetColumnMap.get("WIP Number"))) {
                            if (wip.equals(cell.getDisplayValue())) {
                                logger.info("Processing WIP # {}", cell.getDisplayValue());
                                //row update required
                                List<Cell> cells = new Cell.UpdateRowCellsBuilder()
                                        .addCell(customerProjectSheetColumnMap.get("WIP Status"), cellValues[4], false, null, null)
                                        .addCell(customerProjectSheetColumnMap.get("Product Description"), cellValues[3], false, null, null)
                                        .addCell(customerProjectSheetColumnMap.get("Project Manager"), cellValues[5], false, null, null).build();
                                Row rowx = new Row.UpdateRowBuilder().setRowId(row.getId()).setCells(cells).build();
                                updateRows.add(rowx);
                                foundMatchingWIP = true;

                                break;
                            }
                        }
                    }
                }

            }
            List<Row> addRows = new ArrayList();
            if (!foundMatchingWIP) {


                for (Object o : projectItems) {
                    Object[] arr = (Object[]) o;
                    List<Cell> cells = new Cell.AddRowCellsBuilder()
                            .addCell(customerProjectSheetColumnMap.get("Project Number"), key.toString(), false, null, null)
                            .addCell(customerProjectSheetColumnMap.get("Product Number"), arr[2], false, null, null)
                            .addCell(customerProjectSheetColumnMap.get("WIP Number"), arr[1], false, null, null)
                            .addCell(customerProjectSheetColumnMap.get("Product Description"), arr[3], false, null, null)
                            .addCell(customerProjectSheetColumnMap.get("WIP Status"), arr[4], false, null, null)
                            .addCell(customerProjectSheetColumnMap.get("Project Manager"), arr[5], false, null, null)
                            .build();
                    logger.info("Adding new WIP # {}", arr[1]);
                    Long parentRowId = getParentIdforProjectNumber(parentRows, key.toString());
                    Row row = new Row.AddRowBuilder().setToBottom(true).setParentId(parentRowId).setCells(cells).build();
                    addRows.add(row);


                }

                if (!addRows.isEmpty()) {
                    //add new rows (parent)
                    List<Row> newRowsX = ctxManager.getConnector().getSmartsheet().sheetResources().rowResources().addRows(
                            getCustomerProjectSheetId(),    // sheet Id
                            addRows);

                }

            }


        }


        if (!updateRows.isEmpty()) {
            //add new rows (parent)
            List<Row> newRowsX = ctxManager.getConnector().getSmartsheet().sheetResources().rowResources().updateRows(
                    getCustomerProjectSheetId(),    // sheet Id
                    updateRows);

        }


    }


    /**
     * @param dataMap
     * @return
     */
    private Map<String, List<Object>> buildProjectDataMap(TreeMap<String, List<Object>> dataMap) {
        Map<String, List<Object>> projectDataMap = new HashMap();
        for (Object key : dataMap.keySet()) {
            List<Object> cells = dataMap.get(key);
            List<Object> dmCells = projectDataMap.get(cells.get(0).toString());
            if (dmCells == null) {
                dmCells = new ArrayList();
                projectDataMap.put(cells.get(0).toString(), dmCells);
            }
            dmCells.add(cells.toArray());
        }

        return projectDataMap;
    }

    /**
     * @param file
     * @return
     * @throws java.io.IOException
     */
    private TreeMap<String, List<Object>> buildDataMapFromFileData(File file) throws IOException {
        logger.info("Reading data from file {}", file.getPath());

        FileInputStream excelFile = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(excelFile);
        org.apache.poi.ss.usermodel.Sheet datatypeSheet = workbook.getSheetAt(0);
        Iterator<org.apache.poi.ss.usermodel.Row> iterator = datatypeSheet.iterator();
        TreeMap<String, List<Object>> product = new TreeMap();
        while (iterator.hasNext()) {

            org.apache.poi.ss.usermodel.Row currentRow = iterator.next();
            handleRow(currentRow, product);


        }

        return product;
    }


    /**
     * @param row
     * @param product
     */
    private void handleRow(org.apache.poi.ss.usermodel.Row row, TreeMap<String, List<Object>> product) {

        Iterator<org.apache.poi.ss.usermodel.Cell> cellIterator = row.iterator();

        List<Object> cellValues = new ArrayList();
        while (cellIterator.hasNext()) {
            org.apache.poi.ss.usermodel.Cell currentCell = cellIterator.next();
            handleCell(currentCell, cellValues);
        }

        String exclusionData = ConfigUtil.getConfigItemValue(ctxManager.getConfigInfo(), ConfigSheetConstantNames.DATA_IMPORT_EXCLUSION_FILTERS, ConfigSheetConstantNames.WIP_STATE);
        String[] exclusions = null;
        if (exclusionData != null) {
            exclusions = exclusionData.split(",");
        }

        boolean exclude = false;
        if (exclusions != null) {
            for (String ex : exclusions) {
                if (cellValues.get(4).toString().equalsIgnoreCase(ex)) {
                    exclude = true;
                    break;
                }

            }
        }
        if (!exclude) {
            //add new entry for the product
            product.put(cellValues.get(2).toString(), cellValues);
        }


    }

    /**
     * @param cell
     * @param cellValues
     */
    private void handleCell(org.apache.poi.ss.usermodel.Cell cell, List<Object> cellValues) {
        //getCellTypeEnum shown as deprecated for version 3.15
        //getCellTypeEnum ill be renamed to getCellType starting from version 4.0

        if (cell.getCellTypeEnum() == CellType.STRING) {
            cellValues.add(cell.getStringCellValue());

        } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
            cellValues.add(cell.getNumericCellValue());
        }
    }


    /**
     * @return
     * @throws com.smartsheet.api.SmartsheetException
     * @throws java.io.IOException
     */
    private File createLocalCopyOfLatestAtachment() throws SmartsheetException, IOException {
        //get sheetid for Excel Import Export Data sheet
        ConfigItem item = ConfigUtil.getConfigItem(ctxManager.getConfigInfo(), ConfigSheetConstantNames.SHEET_ID, ConfigSheetConstantNames.EXCEL_IMPORT_EXPORT_DATA);
        Long excelImportExportDataSheetId = ConfigUtil.getSheetIdForHyperLink(item);
        //get the sheet
        Sheet sheet = ctxManager.getConnector().getSheet(excelImportExportDataSheetId);
        //build columnid map
        Map<String, Long> colMap = ConfigUtil.buildColumnIdMap(sheet);
        //find the desired row, which has attachment for data import
        Row row = findA2ProjectListExcelAttachmentRow(sheet, colMap);

        if (row == null) {
            throw new SmartsheetException("\"" + ExcelImportExportItemNames.CUSTOMER_PROJECT_SHEET + "\" Excel Data row not found in Excel Import Export Data sheet");
        }

        //get attachments
        Smartsheet smartsheet = ctxManager.getConnector().getSmartsheet();

        PagedResult<Attachment> attachments = smartsheet.sheetResources().rowResources().attachmentResources().getAttachments(
                sheet.getId(),       // long sheetId
                row.getId(),       // long rowId
                null                     // PaginationParameters
        );

        //create a local file from latest entry (found first)
        // return  new File(ConfigUtil.getConfigItemValue(ctxManager.getConfigInfo(), "Data Import", "Data Import Source File Path")).getAbsoluteFile();
        File file = null;
        for (Attachment attach : attachments.getData()) {
            Attachment attachment = smartsheet.sheetResources().attachmentResources().getAttachment(
                    sheet.getId(),       // long sheetId
                    attach.getId()        // long attachmentId
            );
            file = new File(attachment.getName());
            FileUtils.copyURLToFile(new URL(attachment.getUrl()), file);
            break;

        }

        return file;

    }

    /**
     * @param sheet
     * @param colMap
     * @return
     */
    private Row findA2ProjectListExcelAttachmentRow(Sheet sheet, Map<String, Long> colMap) {
        for (Row row : sheet.getRows()) {
            for (Cell cell : row.getCells()) {
                if (cell.getColumnId().equals(colMap.get(ExcelImportExportItemNames.ITEM))) {
                    if (cell.getDisplayValue().equals(ExcelImportExportItemNames.CUSTOMER_PROJECT_SHEET)) {

                        return row;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return
     * @throws com.smartsheet.api.SmartsheetException
     */
    public Map<String, List<Row>> getCustomerProjectList() throws SmartsheetException {

        //ignore row items
        String ignoreItems = ConfigUtil.getConfigItemValue(ctxManager.getConfigInfo(), ExcelImportExportItemNames.CUSTOMER_PROJECT_SHEET, "Ignore Row Items");
        String[] ignoreItemsArr = {};
        if (ignoreItems != null) {
            ignoreItemsArr = ignoreItems.split(",");
        }

        Long sheetid = getCustomerProjectSheetId();
        Sheet sheet = ctxManager.getConnector().getSheet(sheetid);
        customerProjectSheetColumnMap = ConfigUtil.buildColumnIdMap(sheet);
        List<Row> parentRows = new ArrayList();
        Map<String, List<Row>> rowMap = new HashMap();
        //store parent rows
        rowMap.put("parentRows", parentRows);
        //get primary column id;
        Long primaryColumnId = 0L;
        int primaryColIndex = 0;
        for (Column col : sheet.getColumns()) {
            if (col.getPrimary() == null) {
                continue;
            }
            if (col.getPrimary()) {
                primaryColumnId = col.getId();
                primaryColIndex = col.getIndex();
                break;
            }
        }
        Long parentId = null;
        String projectNumber = null;
        for (Row row : sheet.getRows()) {

            if (row.getParentId() == null) {
                //current row id becomes the parent id.
                parentId = row.getId();
                parentRows.add(row);
                projectNumber = row.getCells().get(primaryColIndex).getDisplayValue();


                if (!Arrays.asList(ignoreItemsArr).contains(projectNumber)) {
                    rowMap.put(projectNumber, new ArrayList<Row>());
                }


            } else {
                if (row.getParentId().equals(parentId)) {
                    List<Row> rowList = rowMap.get(projectNumber);
                    rowList.add(row);
                }
            }


        }
        return rowMap;//customerProjectList;
    }


    private Long getCustomerProjectSheetId() {
        return ConfigUtil.getSheetIdForHyperLink(ConfigUtil.getConfigItem(ctxManager.getConfigInfo(), ConfigSheetConstantNames.SHEET_ID, ExcelImportExportItemNames.CUSTOMER_PROJECT_SHEET));

    }

}
