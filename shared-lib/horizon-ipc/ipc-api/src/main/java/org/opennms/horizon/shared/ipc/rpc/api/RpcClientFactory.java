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

package org.opennms.horizon.shared.ipc.rpc.api;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

/**
 * Creates a {@link RpcClient} that can be used to invoke RPCs against the given {@link RpcModule}.
 *
 * @author jwhite
 */
public interface RpcClientFactory {
    String LOG_PREFIX = "ipc";

    <T extends Message> RpcClient<T> getClient(Deserializer<T> deserializer);
    RpcClient<RpcResponseProto> getClient();

    interface Deserializer<T extends Message> {
        T deserialize(RpcResponseProto response) throws InvalidProtocolBufferException;
    }

}
