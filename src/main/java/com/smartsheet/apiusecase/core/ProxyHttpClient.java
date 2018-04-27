package com.smartsheet.apiusecase.core;

import com.smartsheet.api.internal.http.DefaultHttpClient;
import com.smartsheet.api.internal.http.HttpRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;

public class ProxyHttpClient extends DefaultHttpClient {

    private String proxyHost;
    private Integer proxyPort;

    public ProxyHttpClient(String proxyHost, Integer proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    /** Override this method to inject additional headers, or setup proxy information
     * on the request.
     */
    @Override
    public HttpRequestBase createApacheRequest(HttpRequest smartsheetRequest) {
        HttpRequestBase apacheHttpRequest = super.createApacheRequest(smartsheetRequest);

        RequestConfig.Builder builder = RequestConfig.custom();
        if (apacheHttpRequest.getConfig() != null) {
            builder = RequestConfig.copy(apacheHttpRequest.getConfig());
        }
        HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
        builder.setProxy(proxy);
        RequestConfig config = builder.build();
        apacheHttpRequest.setConfig(config);
        return apacheHttpRequest;
    }
}