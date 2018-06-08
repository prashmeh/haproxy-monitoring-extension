package com.appdynamics.extensions.haproxy;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.haproxy.metrics.Stat;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.opencsv.CSVReader;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;


@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpClientUtils.class)
@PowerMockIgnore("javax.net.ssl.*")
public class HAProxyMonitorTaskTest {

    public static final Logger logger = LoggerFactory.getLogger(HAProxyMonitorTaskTest.class);

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    private Stat.Stats stat;

    private MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("HaProxy Monitor", "Custom Metrics|HAProxy|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));

    private HAProxyMonitorTask haProxyMonitorTask;

    private Map<String, String> expectedValueMap;


    @Before
    public void before() {

        contextConfiguration.setConfigYml("/Users/prashant.mehta/myCode/haproxy-monitoring-extension/src/test/resources/conf/test-config.yml");
        contextConfiguration.setMetricXml("/Users/prashant.mehta/myCode/haproxy-monitoring-extension/src/test/resources/conf/test-metrics.xml", Stat.Stats.class);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        stat = (Stat.Stats) contextConfiguration.getMetricsXml();

        Map configYml = contextConfiguration.getConfigYml();
        Map<String, String> serverArgs = new HashMap<>();
        Map<String, ?> server = (Map<String, ?>) ((List) configYml.get("servers")).get(0);
        for (Map.Entry<String, ?> subServerEntry : server.entrySet()) {
            if (subServerEntry.getValue() instanceof List) {
                String sb = "";
                Iterator itr = ((List) subServerEntry.getValue()).iterator();
                while (itr.hasNext()) {
                    sb += (itr.next().toString() + ',');
                }
                serverArgs.put(subServerEntry.getKey(), sb);
            } else {
                serverArgs.put(subServerEntry.getKey(), (subServerEntry.getValue()).toString());
            }
        }

        haProxyMonitorTask = Mockito.spy(new HAProxyMonitorTask(serviceProvider, contextConfiguration, serverArgs));

        PowerMockito.mockStatic(HttpClientUtils.class);
        PowerMockito.mockStatic(CloseableHttpClient.class);

        PowerMockito.when(HttpClientUtils.getResponseAsStr(any(CloseableHttpClient.class), anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return readFromCSV();
                    }
                });
    }

    private String readFromCSV() throws IOException {
        String responseString = "";
        String csvFile = "/Users/prashant.mehta/myCode/haproxy-monitoring-extension/src/test/resources/demo.csv";

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(csvFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String[] line;
        while ((line = reader.readNext()) != null) {
            String str = "";
            for (int i = 0; i < line.length; i++) {
                str += (line[i] + ',');
            }
            str += '\n';
            responseString += str;
        }
        return responseString;
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

//    @Test
//    public void onTaskComplete() {
//    }


    @Test
    public void testHAProxyResponseRunTest() throws IOException {
        expectedValueMap = getExpectedResultMap();
        haProxyMonitorTask.run();

        validateMetrics();
        Assert.assertTrue("The expected values were not sent. The missing values are " + expectedValueMap, expectedValueMap.isEmpty());
    }

    private void validateMetrics() {
        for (Metric metric : haProxyMonitorTask.getMetrics()) {
            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (expectedValueMap.containsKey(metricName)) {
                String expectedValue = expectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName + " failed", expectedValue, actualValue);
                expectedValueMap.remove(metricName);
            } else {
                System.out.println("\"" + metricName + "\",\"" + actualValue + "\"");
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

    private Map<String, String> getExpectedResultMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|server name", "FRONTEND");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|current sessions", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|max sessions", "42");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|total sessions", "1519275");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|bytes in", "333499537");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|bytes out", "6338133277");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|denied requests", "738400");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|error requests", "16134");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|type", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|rate", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|rate_limit", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|rate_max", "50");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_2xx", "697370");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_3xx", "72283");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_4xx", "41188");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_5xx", "75");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|hrsp_other", "738521");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|req_rate", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|req_rate_max", "50");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|req_tot", "1539497");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|comp_in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|comp_out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|comp_byp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|FRONTEND|comp_rsp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|server name", "IPv4-direct");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|current sessions", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|max sessions", "33");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|total sessions", "808403");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|bytes in", "135050797");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|bytes out", "1188864381");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|denied requests", "729660");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|error requests", "10");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-direct|type", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|server name", "IPv4-cached");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|max sessions", "15");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|total sessions", "652397");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|bytes in", "185024074");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|bytes out", "4716795289");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|denied requests", "1574");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|error requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv4-cached|type", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|server name", "IPv6-direct");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|max sessions", "41");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|total sessions", "45355");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|bytes in", "11023081");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|bytes out", "312920138");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|denied requests", "228");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|error requests", "16124");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|IPv6-direct|type", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|server name", "local");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|max sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|total sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|bytes in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|bytes out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|denied requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|error requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local|type", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|proxy name", "http-in");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|server name", "local-https");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|max sessions", "5");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|total sessions", "21397");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|bytes in", "2401585");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|bytes out", "119553469");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|denied requests", "6938");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|error requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|http-in|local-https|type", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|proxy name", "www");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|server name", "www");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|max_queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|max sessions", "20");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|session limit", "20");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|total sessions", "456944");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|bytes in", "170279920");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|bytes out", "5814720367");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|connection retries", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|active servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|backup servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|checks failed", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|lbtot", "456950");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|type", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|rate_max", "44");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|check_status", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|check_code", "200");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|check_duration", "6");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_2xx", "416068");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_3xx", "16352");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_4xx", "24441");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|hrsp_other", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|client aborts", "26733");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|server abortes", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|lastsess", "5");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|qtime", "OK");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|rtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|www|ttime", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|proxy name", "www");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|server name", "bck");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|max_queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|max sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|session limit", "10");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|total sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|bytes in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|bytes out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|connection retries", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|active servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|backup servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|checks failed", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|lbtot", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|type", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|rate_max", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|check_status", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|check_code", "200");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|check_duration", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_2xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_3xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_4xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|hrsp_other", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|client aborts", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|server abortes", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|lastsess", "-1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|qtime", "OK");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|rtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|bck|ttime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|proxy name", "www");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|server name", "BACKEND");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|max_queued_requests", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|max sessions", "24");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|session limit", "100");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|total sessions", "457291");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|bytes in", "170297227");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|bytes out", "5814781091");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|denied requests", "323");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|connection retries", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|active servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|backup servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|lbtot", "456950");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|type", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|rate_max", "44");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_2xx", "416068");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_3xx", "16352");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_4xx", "24770");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_5xx", "75");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|hrsp_other", "26");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|req_tot", "457291");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|client aborts", "26743");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|server abortes", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|comp_in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|comp_out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|comp_byp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|comp_rsp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|lastsess", "5");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|rtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|www|BACKEND|ttime", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|proxy name", "git");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|server name", "www");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|max_queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|max sessions", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|session limit", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|total sessions", "653");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|bytes in", "304655");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|bytes out", "20066675");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|connection retries", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|active servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|backup servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|checks failed", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|lbtot", "650");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|type", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|rate_max", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|check_status", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|check_code", "200");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|check_duration", "4");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_2xx", "633");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_3xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_4xx", "20");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|hrsp_other", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|client aborts", "33");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|server abortes", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|lastsess", "2902");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|qtime", "OK");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|rtime", "4");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|www|ttime", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|proxy name", "git");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|server name", "bck");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|max_queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|max sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|session limit", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|total sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|bytes in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|bytes out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|connection retries", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|active servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|backup servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|checks failed", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|lbtot", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|type", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|rate_max", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|check_status", "9");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|check_code", "200");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|check_duration", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_2xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_3xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_4xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|hrsp_other", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|client aborts", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|server abortes", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|lastsess", "-1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|qtime", "OK");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|rtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|bck|ttime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|proxy name", "git");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|server name", "BACKEND");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|max_queued_requests", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|current sessions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|max sessions", "3");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|session limit", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|total sessions", "653");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|bytes in", "306399");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|bytes out", "20066675");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|denied requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|connection retries", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|server weight", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|active servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|backup servers", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|total downtime", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|lbtot", "650");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|type", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|rate", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|rate_max", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_2xx", "633");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_3xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_4xx", "20");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|hrsp_other", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|req_tot", "653");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|client aborts", "33");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|server abortes", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|comp_in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|comp_out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|comp_byp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|comp_rsp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|lastsess", "2902");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|rtime", "4");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|git|BACKEND|ttime", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|proxy name", "demo");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|server name", "BACKEND");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|max_queued_requests", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|current sessions", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|max sessions", "6");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|session limit", "20");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|total sessions", "19333");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|bytes in", "6287622");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|bytes out", "454680661");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|denied requests", "40");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|denied responses", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|connection errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|response errors", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|connection retries", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|request redispatches", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|status", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|server weight", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|active servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|backup servers", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|number of transitions", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|last transition", "3952709");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|lbtot", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|type", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|rate", "1");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|rate_max", "5");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_1xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_2xx", "18574");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_3xx", "38");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_4xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_5xx", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|hrsp_other", "720");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|req_tot", "19332");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|client aborts", "72");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|server abortes", "196");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|comp_in", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|comp_out", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|comp_byp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|comp_rsp", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|lastsess", "0");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|rtime", "2");
        map.put("Custom Metrics|HAProxy|Local HA-Proxy|demo|BACKEND|ttime", "0");
        return map;
    }

}