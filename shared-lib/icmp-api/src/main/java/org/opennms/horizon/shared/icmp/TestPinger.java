/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.horizon.shared.icmp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Pinger} that always responds successfully.
 *
 * Should be used exclusively for testing.
 */
public class TestPinger implements Pinger {

    @Override
    public void ping(InetAddress host, long timeout, int retries, int packetsize, int sequenceId, PingResponseCallback cb ) throws Exception {
        cb.handleResponse( host, new EchoPacket() {

            @Override
            public boolean isEchoReply() {
                return true;
            }

            @Override
            public int getIdentifier() {
                return 0;
            }

            @Override
            public int getSequenceNumber() {
                return 0;
            }

            @Override
            public long getThreadId() {
                return 0;
            }

            @Override
            public long getReceivedTimeNanos() {
                return 0;
            }

            @Override
            public long getSentTimeNanos() {
                return 0;
            }

            @Override
            public double elapsedTime(TimeUnit timeUnit) {
                return 0;
            }
        });
    }

    @Override
    public void ping(InetAddress host, long timeout, int retries, int sequenceId, PingResponseCallback cb) throws Exception {
        ping( host, timeout, retries, 0, sequenceId, cb );
    }

    @Override
    public Number ping(InetAddress host, long timeout, int retries, int packetsize) throws Exception {
        return 1;
    }

    @Override
    public Number ping(InetAddress host, long timeout, int retries) throws Exception {
        return 1;
    }

    @Override
    public Number ping(InetAddress host) throws Exception {
        return 1;
    }

    @Override
    public List<Number> parallelPing(InetAddress host, int count, long timeout, long pingInterval) {
        final List<Number> numbers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            numbers.add(1d);
        }
        return numbers;
    }

    @Override
    public List<Number> parallelPing(InetAddress host, int count, long timeout, long pingInterval, int size) {
        return parallelPing(host, count, timeout, pingInterval);
    }

    @Override
    public void initialize4() {
        // pass
    }

    @Override
    public void initialize6() {
        // pass
    }

    @Override
    public boolean isV4Available() {
        return true;
    }

    @Override
    public boolean isV6Available() {
        return true;
    }

    @Override
    public void setAllowFragmentation(boolean allow) {
        // pass
    }

    @Override
    public void setTrafficClass(int tc) {
        // pass
    }
}
