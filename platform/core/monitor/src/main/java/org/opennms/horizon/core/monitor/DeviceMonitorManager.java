/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.core.monitor;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.opennms.horizon.db.dao.api.IpInterfaceDao;
import org.opennms.horizon.db.dao.api.MonitoringLocationDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsIpInterface;
import org.opennms.horizon.db.model.OnmsMonitoringLocation;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.netmgt.icmp.proxy.LocationAwarePingClient;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.proxy.LocationAwareSnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DeviceMonitorManager implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceMonitorManager.class);
    private static final String SYS_OBJECTID_INSTANCE = ".1.3.6.1.2.1.1.2.0";
    private static final Long INVALID_UP_TIME = -1L;
    private static final long DEVICE_INITIAL_DELAY = 5; // 5 sec
    private static final long DEVICE_INTERVAL = 30; // 30 sec
    private static final long DEVICE_TTL = 20; //20 sec
    private static final int SNMP_TIMEOUT_DEFAULT = 18000; // 18 sec
    private static final int SNMP_RETRIES_DEFAULT = 1;
    private final LocationAwarePingClient locationAwarePingClient;
    private final LocationAwareSnmpClient locationAwareSnmpClient;
    private final EventSubscriptionService eventSubscriptionService;
    private final NodeDao nodeDao;
    private final IpInterfaceDao ipInterfaceDao;
    private final SessionUtils sessionUtils;
    private final OnmsMetricsAdapter metricsAdapter;
    private final MonitoringLocationDao monitoringLocationDao;
    private final List<OnmsNode> nodeCache = new ArrayList<>();
    private final ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("device-monitor-runner-%d")
        .build();
    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private static final String[] labelNames = {"instance", "location"};
    private final Gauge rttGauge = Gauge.build().name("icmp_round_trip_time").help("ICMP round trip time")
        .unit("msec").labelNames(labelNames).register(collectorRegistry);
    private final Gauge upTimeGauge = Gauge.build().name("snmp_uptime").help("SNMP Up Time")
        .unit("sec").labelNames(labelNames).register(collectorRegistry);
    // Cache SNMP Uptime for each Interface.
    private final Map<String, Long> snmpUpTimeCache = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(25, monitorThreadFactory);


    public DeviceMonitorManager(EventSubscriptionService eventSubscriptionService,
                                NodeDao nodeDao,
                                IpInterfaceDao ipInterfaceDao,
                                SessionUtils sessionUtils,
                                LocationAwarePingClient locationAwarePingClient,
                                LocationAwareSnmpClient locationAwareSnmpClient,
                                OnmsMetricsAdapter metricsAdapter, MonitoringLocationDao locationDao) {
        this.eventSubscriptionService = eventSubscriptionService;
        this.nodeDao = nodeDao;
        this.ipInterfaceDao = ipInterfaceDao;
        this.sessionUtils = sessionUtils;
        this.locationAwarePingClient = locationAwarePingClient;
        this.locationAwareSnmpClient = locationAwareSnmpClient;
        this.metricsAdapter = metricsAdapter;
        this.monitoringLocationDao = locationDao;
    }

    public void init() {
        eventSubscriptionService.addEventListener(this);
        // Add all nodes currently present in the inventory to cache.
        sessionUtils.withReadOnlyTransaction(() -> nodeCache.addAll(nodeDao.findAll()));
        nodeCache.forEach(onmsNode -> {
            LOG.info("Starting device monitoring for device with ID {}", onmsNode.getId());
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMonitors(onmsNode), DEVICE_INITIAL_DELAY, DEVICE_INTERVAL, TimeUnit.SECONDS);
        });
    }


    private void runMonitors(OnmsNode onmsNode) {
        try {
            List<OnmsIpInterface> ipInterfaces = sessionUtils.withReadOnlyTransaction(() -> ipInterfaceDao.findInterfacesByNodeId(onmsNode.getId()));
            String location = sessionUtils.withReadOnlyTransaction(() -> onmsNode.getLocation().getLocationName());
            ipInterfaces.forEach(onmsIpInterface -> {
                LOG.info("Polling ICMP/SNMP Monitor for IPAddress {}", onmsIpInterface.getIpAddress());
                pollIcmp(onmsIpInterface.getIpAddress(), location);
                pollSnmp(onmsIpInterface.getIpAddress(), onmsNode.getSnmpCommunityString(), location);
            });
        } catch (Exception e) {
            LOG.error("Exception while running monitors for device with Id : {}", onmsNode.getId(), e);
        }
    }

    private void pollIcmp(InetAddress inetAddress, String location) {
        try {
            locationAwarePingClient.ping(inetAddress).withLocation(location)
                .withTimeout(DEVICE_TTL, TimeUnit.SECONDS)
                .execute()
                .whenComplete(((pingSummary, throwable) -> {
                    if (throwable != null) {
                        LOG.info("ICMP is Down at IpAddress {}", inetAddress.getHostAddress());
                        LOG.error("Exception while pinging IpAddress {}", inetAddress.getHostAddress(), throwable);
                    } else if (pingSummary != null && pingSummary.getSequence(0) != null) {
                        // we are only pinging one IpAddress, use sequence 0
                        double icmpResponseTime = pingSummary.getSequence(0).getResponse().getRtt();
                        if (icmpResponseTime != Double.POSITIVE_INFINITY) {
                            LOG.info("ICMP Round trip time for IPAddress {} at location {} : {} msec",
                                inetAddress.getHostAddress(), location, icmpResponseTime);
                            updateIcmpMetric(icmpResponseTime, inetAddress.getHostAddress(), location);
                        } else {
                            LOG.info("ICMP is Down at IpAddress {}", inetAddress.getHostAddress());
                        }
                    }
                }));
        } catch (Exception e) {
            LOG.error("Exception while executing ping for {}", inetAddress, e);
        }
    }

    private void pollSnmp(InetAddress inetAddress, String snmpCommunityString, String location) {
        try {
            final SnmpAgentConfig agentConfig = new SnmpAgentConfig();
            agentConfig.setAddress(inetAddress);
            if(!Strings.isNullOrEmpty(snmpCommunityString)) {
                agentConfig.setReadCommunity(snmpCommunityString);
            }
            agentConfig.setTimeout(SNMP_TIMEOUT_DEFAULT);
            agentConfig.setRetries(SNMP_RETRIES_DEFAULT);
            locationAwareSnmpClient.get(agentConfig, SnmpObjId.get(SYS_OBJECTID_INSTANCE))
                .withLocation(location)
                .withDescription("Device-Monitor")
                .withTimeToLive(DEVICE_TTL * 1000)
                .execute().whenComplete(((snmpValue, throwable) -> {
                    if (throwable != null) {
                        LOG.info("SNMP is Down at IpAddress {}", inetAddress.getHostAddress());
                        LOG.debug("Exception while detecting Snmp service at IpAddress {}", inetAddress.getHostAddress(), throwable);
                        updateSnmpMetrics(inetAddress.getHostAddress(), false, location);
                    } else if (snmpValue != null && !snmpValue.isError()) {
                        LOG.info("SNMP is Up at IpAddress {}", inetAddress.getHostAddress());
                        updateSnmpMetrics(inetAddress.getHostAddress(), true, location);
                    }
                }));
        } catch (Exception e) {
            LOG.error("Exception while detecting SNMP service on IpAddress {}", inetAddress);
        }
    }

    private void updateIcmpMetric(double responseTime, String ipAddress, String location) {
        String[] labelValues = {ipAddress, location};
        var groupingKey = IntStream.range(0, labelNames.length).boxed()
            .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));
        rttGauge.labels(labelValues).set(responseTime);
        metricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }

    private void updateSnmpMetrics(String ipAddress, boolean status, String location) {
        String[] labelValues = {ipAddress, location};
        if (status) {
            Long firstUpTime = snmpUpTimeCache.get(ipAddress);
            long totalUpTimeInMsec = 0;
            if (firstUpTime != null && firstUpTime.longValue() != INVALID_UP_TIME) {
                totalUpTimeInMsec = System.currentTimeMillis() - firstUpTime;
            } else {
                snmpUpTimeCache.put(ipAddress, System.currentTimeMillis());
            }
            long totalUpTimeInSec = TimeUnit.MILLISECONDS.toSeconds(totalUpTimeInMsec);
            upTimeGauge.labels(labelValues).set(totalUpTimeInSec);
            LOG.info("Total upTime of SNMP for {} at location {} : {} sec",
                ipAddress, location, totalUpTimeInSec);
        } else {
            snmpUpTimeCache.put(ipAddress, INVALID_UP_TIME);
        }
        var groupingKey = IntStream.range(0, labelNames.length).boxed()
            .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));
        metricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }

    @Override
    public String getName() {
        return "Device-Monitor-Manager";
    }

    @Override
    public void onEvent(IEvent event) {
        if (event.getUei().equals(EventConstants.NODE_ADDED_EVENT_UEI)) {
            Long nodeId = event.getNodeid();
            if (nodeId != null) {
                OnmsNode node = sessionUtils.withReadOnlyTransaction(() -> nodeDao.get(nodeId.intValue()));
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMonitors(node), DEVICE_INITIAL_DELAY, DEVICE_INTERVAL, TimeUnit.SECONDS);
            }
        }
        if (event.getUei().equals(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)) {
            String ipInterface = event.getInterface();
            String location = event.getParm(EventConstants.PARM_LOCATION).isValid() ?
                event.getParm(EventConstants.PARM_LOCATION).getValue().getContent() : MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID;
            createNodeFromDiscovery(ipInterface, location);
        }
    }

    public void shutdown() {
        eventSubscriptionService.removeEventListener(this);
        scheduledThreadPoolExecutor.shutdown();
    }

    public void createNodeFromDiscovery(String ipInterface, String location) {
        OnmsMonitoringLocation monitoringLocation = sessionUtils.withReadOnlyTransaction(() -> monitoringLocationDao.get(location));
        OnmsNode onmsNode = new OnmsNode(monitoringLocation, ipInterface);
        OnmsIpInterface onmsIpInterface = new OnmsIpInterface(ipInterface);
        onmsNode.addIpInterface(onmsIpInterface);
        onmsNode.setCreateTime(new Date());
        sessionUtils.withTransaction(() -> nodeDao.saveOrUpdate(onmsNode));
        LOG.info("Node created from discovery with IpAddress {}", ipInterface);
        List<OnmsNode> nodes = sessionUtils.withReadOnlyTransaction(() -> nodeDao.findByLabel(ipInterface));
        if (!nodes.isEmpty()) {
            OnmsNode node = nodes.get(0);
            runMonitors(node);
        }
    }

}
