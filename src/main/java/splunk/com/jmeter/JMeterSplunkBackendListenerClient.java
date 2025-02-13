package splunk.com.jmeter;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import splunk.com.jmeter.config.splunk.SplunkConfig;

import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * splunk jmeter plugin.
 *
 *
 */

public class JMeterSplunkBackendListenerClient extends AbstractBackendListenerClient {

    SplunkConfig splunkConfig;
    private String runId;
    private long offset;
    private static final String TIMESTAMP = "timestamp";
    private int activecount = 1;
    private String splunkhost;
    private String splunkPort;
    private String splunkHTTPScheme = "HTTPS";
    private String splunkIndex;
    private String splunkToken;
    private String splunkSourceType;



    @Override
    public void setupTest(BackendListenerContext context) throws Exception {

        splunkhost = context.getParameter("splunkHost");
        splunkPort = context.getParameter("splunkPort");
        splunkIndex = context.getParameter("splunkIndex");
        splunkToken = context.getParameter("splunkToken");
        splunkSourceType = context.getParameter("splunkSourceType");
        String dateTimeAppendFormat = context.getParameter("dateTimeAppendFormat");
        String normalizedTime = context.getParameter("normalizedTime");

        if (dateTimeAppendFormat != null && dateTimeAppendFormat.trim().equals("")) {
            dateTimeAppendFormat = null;
        }

        if (normalizedTime != null && normalizedTime.trim().length() > 0) {
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
            Date d = sdf2.parse(normalizedTime);
            long normalizedDate = d.getTime();
            Date now = new Date();
            offset = now.getTime() - normalizedDate;
        }
        runId = context.getParameter("runId");
        super.setupTest(context);
    }


    @SuppressWarnings("deprecation")
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {


        for (SampleResult result : results) {
            JsonObject builder = Json.createObjectBuilder()
                    .add("ActiveCount", activecount++)
                    .add("ResponseTime", result.getTime())
                    .add("ElapsedTime", result.getTime())
                    .add("Label", result.getSampleLabel())
                    .add("ResponseCode", result.getResponseCode())
                    .add("ResponseMessage", result.getResponseMessage())
                    .add("ThreadName", result.getThreadName())
                    .add("DataType", result.getDataType())
                    .add("Success", String.valueOf(result.isSuccessful()))
                    .add("GrpThreads", result.getGroupThreads())
                    .add("AllThreads", result.getAllThreads())
                    .add("URL", result.getUrlAsString())
                    .add("Latency", result.getLatency())
                    .add("ConnectTime", result.getConnectTime())
                    .add("SampleCount", result.getSampleCount())
                    .add("ErrorCount", result.getErrorCount())
                    .add("Bytes", result.getBytes())
                    .add("BodySize", result.getBodySize())
                    .add("ContentType", result.getContentType())
                    .add("IdleTime", result.getIdleTime())
                    .add(TIMESTAMP, new Date(result.getTimeStamp()).toString())
                    .add("StartTime", new Date(result.getStartTime()).toString())
                    .add("EndTime", new Date(result.getEndTime()).toString())
                    .add("RunId", runId).build();



            SSLContext sslContext = null;
            try {
                sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                        .build();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

/*
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .setConnectionManager(
                            new PoolingHttpClientConnectionManager(
                                    RegistryBuilder.<ConnectionSocketFactory>create()
                                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                                            .register("https", new SSLConnectionSocketFactory(sslContext,
                                                    NoopHostnameVerifier.INSTANCE))
                                            .build()
                            ))
                    .build();
*/
            String proxyHost  = "zscaler-vse.edwardjones.com";
            int proxyPort = 443;
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setProxy(proxy).
                    setSSLContext(sslContext)
                    .setConnectionManager(new PoolingHttpClientConnectionManager(
                            RegistryBuilder.<ConnectionSocketFactory>create()
                                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                                    .register("https", new SSLConnectionSocketFactory(sslContext,
                                            NoopHostnameVerifier.INSTANCE))
                                    .build()
                    ))
                    .build()) {
                HttpPost httppost = new HttpPost(splunkHTTPScheme + "://" + splunkhost + "/services/collector/event/1.0");
                httppost.addHeader("Authorization", " Splunk " + splunkToken);
                String eventStr = "{\"sourcetype\": \"" + splunkSourceType + "\", \"index\":\"" + splunkIndex + "\",\"event\":" + builder + "}";

                    httppost.setEntity(new StringEntity(eventStr));
                    HttpResponse response = httpClient.execute(httppost);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("runId", "${__UUID()}");
        arguments.addArgument("dateTimeAppendFormat", "-yyyy-MM-DD");
        arguments.addArgument("normalizedTime", "2015-01-01 00:00:00.000-00:00");
        arguments.addArgument(SplunkConfig.KEY_SPLUNK_HOST, "localhost");
        arguments.addArgument(SplunkConfig.KEY_SPLUNK_PORT, SplunkConfig.DEFAULT_PORT);
        arguments.addArgument(SplunkConfig.KEY_SPLUNK_TOKEN, "");
        arguments.addArgument(SplunkConfig.KEY_SPLUNK_INDEX, "");
        arguments.addArgument(SplunkConfig.KEY_SPLUNK_SOURCETYPE, "");
        arguments.addArgument(SplunkConfig.KEY_RETENTION_POLICY, SplunkConfig.DEFAULT_RETENTION_POLICY);
        return arguments;
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        super.teardownTest(context);
    }
}