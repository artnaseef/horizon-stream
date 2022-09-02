package org.opennms.horizon.core.monitor;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.opennms.horizon.db.dao.api.MinionDao;
import org.opennms.horizon.db.dao.api.MonitoringLocationDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMinion;
import org.opennms.horizon.echo.EchoRequest;
import org.opennms.horizon.echo.EchoResponse;
import org.opennms.horizon.echo.EchoRpcModule;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.events.model.IParm;
import org.opennms.horizon.ipc.rpc.api.RpcClient;
import org.opennms.horizon.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MinionRpcMonitorManager implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(MinionRpcMonitorManager.class);
    private final static int DEFAULT_MESSAGE_SIZE = 1024;
    private static final String[] LABEL_NAMES = {"instance", "location"};
    private static final int MONITOR_INITIAL_DELAY = 3_000;
    private static final int MONITOR_PERIOD = 30_000;

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();

    private final Gauge responseTimeGauge = Gauge.build().name("minion_response_time").help("Response time of Minion RPC")
        .unit("msec").labelNames(LABEL_NAMES).register(collectorRegistry);

    private final RpcClientFactory rpcClientFactory;
    private final OnmsMetricsAdapter metricsAdapter;
    private final MinionDao minionDao;
    private final SessionUtils sessionUtils;
    private final EventSubscriptionService eventSubscriptionService;
    private final List<OnmsMinion> minionCache = new ArrayList<>();
    private final ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("minion-monitor-runner-%d")
        .build();
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(10, monitorThreadFactory);

    public MinionRpcMonitorManager(RpcClientFactory rpcClientFactory,
                                   OnmsMetricsAdapter metricsAdapter,
                                   SessionUtils sessionUtils,
                                   MinionDao minionDao,
                                   EventSubscriptionService eventSubscriptionService) {
        this.rpcClientFactory = rpcClientFactory;
        this.metricsAdapter = metricsAdapter;
        this.minionDao = minionDao;
        this.sessionUtils = sessionUtils;
        this.eventSubscriptionService = eventSubscriptionService;
    }

    public void init() {
        eventSubscriptionService.addEventListener(this);
        List<OnmsMinion> minions = sessionUtils.withReadOnlyTransaction(minionDao::findAll);
        minionCache.addAll(minions);
        minionCache.forEach(minion -> {
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMinionMonitor(minion.getLocation(), minion.getId()),
                MONITOR_INITIAL_DELAY, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
        });
    }

    public void shutdown() {
        eventSubscriptionService.removeEventListener(this);
        scheduledThreadPoolExecutor.shutdown();
    }

    @Override
    public String getName() {
        return "Minion-Monitor-Manager";
    }

    @Override
    public void onEvent(IEvent event) {

        if (event.getUei().equals(EventConstants.MONITORING_SYSTEM_ADDED_UEI)) {
            IParm locationParm = event.getParm(EventConstants.PARAM_MONITORING_SYSTEM_LOCATION);
            String location = locationParm.getValue() != null ? locationParm.getValue().getContent()
                : MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID;
            IParm systemIdParm = event.getParm(EventConstants.PARAM_MONITORING_SYSTEM_ID);
            String systemId = systemIdParm.getValue() != null ? systemIdParm.getValue().getContent() : null;
            if (!Strings.isNullOrEmpty(systemId)) {
                LOG.info("Received event for new Minion with Id {}", systemId);
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMinionMonitor(location, systemId),
                    MONITOR_INITIAL_DELAY, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
            }
        }

    }


    private void runMinionMonitor(String location, String minionId) {
        LOG.info("test Minion RPC Monitor: executing check for minion: id={}; location={}", minionId, location);
        // Create the client
        RpcClient<EchoRequest, EchoResponse> client = rpcClientFactory.getClient(EchoRpcModule.INSTANCE);

        // Build the request
        EchoRequest request = new EchoRequest();
        request.setId(System.nanoTime() / 1000000L);
        request.setMessage(Strings.repeat("*", DEFAULT_MESSAGE_SIZE));
        request.setLocation(location);
        request.setSystemId(minionId);
        request.setTimeToLiveMs(null);

        try {
            EchoResponse response = client.execute(request).get();
            long responseTime = (System.nanoTime() / 1000000) - response.getId();
            LOG.info("ECHO RESPONSE: node-id={}; node-location={}; duration={}ms", minionId, location, responseTime);
            updateMetrics(responseTime, new String[]{minionId, location});
        } catch (InterruptedException | ExecutionException t) {
            LOG.warn("ECHO REQUEST failed", t);
            LOG.error("Minion RPC Monitor: check for minion failed: id={}", minionId);
        }
    }

    private void updateMetrics(long responseTime, String[] labelValues) {
        responseTimeGauge.labels(labelValues).set(responseTime);
        var groupingKey = IntStream.range(0, LABEL_NAMES.length).boxed()
            .collect(Collectors.toMap(i -> LABEL_NAMES[i], i -> labelValues[i]));
        metricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }

}
