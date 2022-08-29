/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.icmp.proxy.common;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.icmp.EchoPacket;
import org.opennms.netmgt.icmp.PingResponseCallback;

public class PingResultTracker extends CompletableFuture<PingResponseDTO> implements PingResponseCallback {

    @Override
    public void handleResponse(InetAddress address, EchoPacket response) {
        PingResponseDTO responseDTO = new PingResponseDTO();
        responseDTO.setRtt(response.elapsedTime(TimeUnit.MILLISECONDS));
        complete(responseDTO);
    }

    @Override
    public void handleTimeout(InetAddress address, EchoPacket request) {
        PingResponseDTO responseDTO = new PingResponseDTO();
        responseDTO.setRtt(Double.POSITIVE_INFINITY);
        if (!isDone()) {
            complete(responseDTO);
        }
    }

    @Override
    public void handleError(InetAddress address, EchoPacket request, Throwable t) {
        if (!isDone()) {
            completeExceptionally(t);
        }
    }
}