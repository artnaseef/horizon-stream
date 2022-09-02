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

package org.opennms.minion.heartbeat;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.opennms.horizon.db.dao.api.MinionDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMinion;
import org.opennms.horizon.db.model.OnmsMonitoringSystem;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventForwarder;
import org.opennms.horizon.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.ipc.sink.api.SinkModule;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HeartbeatConsumer implements MessageConsumer<MinionIdentityDTO, MinionIdentityDTO> {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatConsumer.class);
    private final MessageConsumerManager messageConsumerManager;
    private final MinionDao minionDao;
    private final SessionUtils sessionUtils;
    private final OnmsMetricsAdapter onmsMetricsAdapter;
    private final EventForwarder eventForwarder;
    private final Map<String, Long> minionUpTime = new ConcurrentHashMap<>();
    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private static final String[] labelNames = {"instance", "location"};
    private final Gauge upTimeGauge = Gauge.build().name("minion_uptime").help("Total Uptime of Minion.")
        .unit("sec").labelNames(labelNames).register(collectorRegistry);

    public HeartbeatConsumer(MessageConsumerManager messageConsumerManager, MinionDao minionDao,
                             SessionUtils sessionUtils, OnmsMetricsAdapter onmsMetricsAdapter,
                             EventForwarder eventForwarder) {
        this.messageConsumerManager = messageConsumerManager;
        this.minionDao = minionDao;
        this.sessionUtils = sessionUtils;
        this.onmsMetricsAdapter = onmsMetricsAdapter;
        this.eventForwarder = eventForwarder;
    }

    public void init() throws Exception {
        messageConsumerManager.registerConsumer(this);
    }

    @Override
    public SinkModule<MinionIdentityDTO, MinionIdentityDTO> getModule() {
        return new HeartbeatModule();
    }

    @Override
    public void handleMessage(MinionIdentityDTO minionHandle) {
        LOG.info("Received heartbeat for Minion with id: {} at location: {}",
            minionHandle.getId(), minionHandle.getLocation());
        OnmsMinion minion = sessionUtils.withReadOnlyTransaction(() -> minionDao.findById(minionHandle.getId()));
        long heartBeatDelta = 0;
        boolean newMinionDiscovered = false;
        if (minion == null) {
            minion = new OnmsMinion();
            minion.setId(minionHandle.getId());
            minion.setLocation(minionHandle.getLocation());
            newMinionDiscovered = true;
        } else {
            heartBeatDelta = minionHandle.getTimestamp().getTime() - minion.getLastUpdated().getTime();
        }
        if (minionHandle.getTimestamp() == null) {
            // The heartbeat does not contain a timestamp - use the current time
            minion.setLastUpdated(new Date());
            LOG.info("Received heartbeat without a timestamp: {}", minionHandle);
        } else if (minion.getLastUpdated() == null) {
            // The heartbeat does contain a timestamp, and we don't have
            // one set yet, so use whatever we've been given
            minion.setLastUpdated(minionHandle.getTimestamp());
        } else if (minionHandle.getTimestamp().after(minion.getLastUpdated())) {
            // The timestamp in the heartbeat is more recent than the one we
            // have stored, so update it
            minion.setLastUpdated(minionHandle.getTimestamp());
        } else {
            // The timestamp in the heartbeat is earlier than the
            // timestamp we have stored, so ignore it
            LOG.info("Ignoring stale timestamp from heartbeat: {}", minionHandle);
        }
        final var updatedMinion = minion;
        sessionUtils.withTransaction(() -> minionDao.saveOrUpdate(updatedMinion));

        if (newMinionDiscovered) {
            sendMinionAddedEvent(minionHandle);
        }

        // Negative value indicates stale update for heartbeat
        if (heartBeatDelta >= 0) {
            updateMetrics(heartBeatDelta, minionHandle.getId(),
                new String[]{minionHandle.getId(), minionHandle.getLocation()});
        }
    }

    private void sendMinionAddedEvent(MinionIdentityDTO minionHandle) {
        final EventBuilder eventBuilder = new EventBuilder(EventConstants.MONITORING_SYSTEM_ADDED_UEI,
            "Horizon.Minion.Heartbeat");
        eventBuilder.addParam(EventConstants.PARAM_MONITORING_SYSTEM_TYPE, OnmsMonitoringSystem.TYPE_MINION);
        eventBuilder.addParam(EventConstants.PARAM_MONITORING_SYSTEM_ID, minionHandle.getId());
        eventBuilder.addParam(EventConstants.PARAM_MONITORING_SYSTEM_LOCATION, minionHandle.getLocation());
        eventForwarder.sendNowSync(eventBuilder.getEvent());
    }

    private void updateMetrics(long heartBeatDelta, String minionId, String[] labelValues) {
        Long lastUpTimeInMsec = minionUpTime.get(minionId);
        long totalUpTimeInMsec = heartBeatDelta;
        if (lastUpTimeInMsec != null) {
            totalUpTimeInMsec = lastUpTimeInMsec + heartBeatDelta;
        }
        long totalUpTimeInSec = TimeUnit.MILLISECONDS.toSeconds(totalUpTimeInMsec);
        upTimeGauge.labels(labelValues).set(totalUpTimeInSec);
        var groupingKey = IntStream.range(0, labelNames.length).boxed()
            .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));
        onmsMetricsAdapter.pushMetrics(collectorRegistry, groupingKey);
        minionUpTime.put(minionId, totalUpTimeInMsec);
    }
}
