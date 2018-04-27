package com.smartsheet.apiusecase.core;

import com.smartsheet.api.Smartsheet;
import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.SmartsheetFactory;
import com.smartsheet.api.models.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartSheetConnector {

    final private static Logger logger = LoggerFactory.getLogger(SmartSheetConnector.class);


    private Smartsheet smartsheet;

    public SmartSheetConnector(String accessToken) {
        this.initialize(accessToken);
    }


    private void initialize(String accessToken) {
        String useProxy = System.getProperty("http.proxySet");
        if (useProxy == null) {
            useProxy = "false";
        }
        if (useProxy.equals("false")) {
            logger.info("Proxy not configured, will use default client");
            smartsheet = SmartsheetFactory.createDefaultClient();

        } else {
            logger.info("Proxy configuration required, will proxy client");
            String server = System.getProperty("http.proxyHost");
            int port = Integer.parseInt(System.getProperty("http.proxyPort"));
            ProxyHttpClient proxyHttpClient = new ProxyHttpClient(server, port);
            smartsheet = SmartsheetFactory.custom().setHttpClient(proxyHttpClient).build();

        }
        smartsheet.setAccessToken(accessToken);

    }

    public Sheet getSheet(long sheetId) throws SmartsheetException {


        Sheet sheet = smartsheet.sheetResources().getSheet(
                sheetId,                // long sheetId
                null,                   // EnumSet<SheetInclusion> includes
                null,                   // EnumSet<ObjectExclusion> excludes
                null,                   // Set<Long> rowIds
                null,                   // Set<Integer> rowNumbers
                null,                   // Set<Long> columnIds
                null,                   // Integer pageSize
                null                    // Integer page
        );


        return sheet;

    }

    public Smartsheet getSmartsheet() {
        return smartsheet;
    }
}
