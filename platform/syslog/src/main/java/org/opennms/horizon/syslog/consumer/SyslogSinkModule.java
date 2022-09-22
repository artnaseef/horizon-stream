/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.horizon.syslog.consumer;


import org.opennms.horizon.db.model.OnmsDistPoller;
import org.opennms.horizon.ipc.sink.api.AbstractXmlSinkModule;
import org.opennms.horizon.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.syslog.api.SyslogConnection;
import org.opennms.horizon.syslog.api.SyslogMessageDTO;
import org.opennms.horizon.syslog.api.SyslogMessageLogDTO;
import org.opennms.horizon.syslog.config.SyslogdConfig;

import java.net.InetSocketAddress;
import java.util.Objects;

public class SyslogSinkModule extends AbstractXmlSinkModule<SyslogConnection, SyslogMessageLogDTO> {

    public static final String MODULE_ID = "Syslog";

    private final SyslogdConfig config;
    private final OnmsDistPoller onmsDistPoller;

    public SyslogSinkModule(SyslogdConfig config, OnmsDistPoller onmsDistPoller) {
        super(SyslogMessageLogDTO.class);
        this.config = Objects.requireNonNull(config);
        this.onmsDistPoller = Objects.requireNonNull(onmsDistPoller);
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return config.getNumThreads();
    }

    @Override
    public AggregationPolicy<SyslogConnection, SyslogMessageLogDTO, SyslogMessageLogDTO> getAggregationPolicy() {
        final String systemId = onmsDistPoller.getId();
        final String systemLocation = onmsDistPoller.getLocation();
        return new AggregationPolicy<SyslogConnection, SyslogMessageLogDTO, SyslogMessageLogDTO>() {
            @Override
            public int getCompletionSize() {
                return config.getBatchSize();
            }

            @Override
            public int getCompletionIntervalMs() {
                return config.getBatchIntervalMs();
            }

            @Override
            public Object key(SyslogConnection syslogConnection) {
                return syslogConnection.getSource();
            }

            @Override
            public SyslogMessageLogDTO aggregate(SyslogMessageLogDTO accumulator, SyslogConnection connection) {
                if (accumulator == null) {
                    accumulator = new SyslogMessageLogDTO(systemLocation, systemId, connection.getSource());
                }
                SyslogMessageDTO messageDTO = new SyslogMessageDTO(connection.getBuffer());
                accumulator.getMessages().add(messageDTO);
                return accumulator;
            }

            @Override
            public SyslogMessageLogDTO build(SyslogMessageLogDTO accumulator) {
                return accumulator;
            }
        };
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return config.getQueueSize();
            }

            @Override
            public int getNumThreads() {
                return config.getNumThreads();
            }

            @Override
            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }

    @Override
    public SyslogConnection unmarshalSingleMessage(byte[] bytes) {
        SyslogMessageLogDTO syslogMessageLogDTO = unmarshal(bytes);
        SyslogMessageDTO syslogMessageDTO = syslogMessageLogDTO.getMessages().get(0);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(syslogMessageLogDTO.getSourceAddress(), syslogMessageLogDTO.getSourcePort());
        return new SyslogConnection(inetSocketAddress, syslogMessageDTO.getBytes());
    }

    /**
     * Used for testing.
     */
    public SyslogMessageLogDTO toMessageLog(SyslogConnection... connections) {
        final String systemId = onmsDistPoller.getId();
        final String systemLocation = onmsDistPoller.getLocation();
        if (connections.length < 1) {
            throw new IllegalArgumentException("One or more connection are required.");
        }
        final SyslogMessageLogDTO messageLog = new SyslogMessageLogDTO(systemLocation, systemId,
                connections[0].getSource());
        for (SyslogConnection connection : connections) {
            final SyslogMessageDTO messageDTO = new SyslogMessageDTO(connection.getBuffer());
            messageLog.getMessages().add(messageDTO);
        }
        return messageLog;
    }

    @Override
    public int hashCode() {
        return Objects.hash(MODULE_ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
}
