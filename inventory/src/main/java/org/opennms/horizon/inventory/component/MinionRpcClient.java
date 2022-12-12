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

package org.opennms.horizon.inventory.component;

import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.springframework.beans.factory.annotation.Qualifier;

import io.grpc.ManagedChannel;

public class MinionRpcClient {
    private final ManagedChannel channel;

    public MinionRpcClient(@Qualifier("minion-gateway") ManagedChannel channel) {
        this.channel = channel;
    }

    private RpcRequestServiceGrpc.RpcRequestServiceBlockingStub rpcStub;

    protected void init() {
        rpcStub = RpcRequestServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if(channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public RpcResponseProto sendRpcRequest(RpcRequestProto request) {
        return rpcStub.request(request);
    }
}