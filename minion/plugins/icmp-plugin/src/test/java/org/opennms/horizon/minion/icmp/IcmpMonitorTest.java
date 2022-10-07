package org.opennms.horizon.minion.icmp;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.minion.icmp.best.BestMatchPingerFactory;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class IcmpMonitorTest {
    @Mock
    MonitoredService monitoredService;

    IcmpMonitorRequest testEchoRequest;
    Any testConfig;
    IcmpMonitor icmpMonitor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(monitoredService.getAddress()).thenReturn(InetAddressUtils.addr("127.0.0.1"));
        icmpMonitor = new IcmpMonitor(new BestMatchPingerFactory());

        testEchoRequest =
            IcmpMonitorRequest.newBuilder()
                .setHost("127.0.0.1")
                .build();

        testConfig = Any.pack(testEchoRequest);
    }

    @Test
    @Ignore("not stable across platforms")
    public void poll() throws Exception {
        CompletableFuture<ServiceMonitorResponse> response = icmpMonitor.poll(monitoredService, testConfig);

        ServiceMonitorResponse serviceMonitorResponse = response.get();

        assertEquals(Status.Up, serviceMonitorResponse.getStatus());
        assertTrue(serviceMonitorResponse.getResponseTime() > 0.0);
    }
}
