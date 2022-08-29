/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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

package org.opennms.miniongateway.grpc.twin;

import com.google.protobuf.Any;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.function.BiConsumer;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.core.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.grpc.stub.StreamObserver;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

public class GrpcTwinPublisher extends AbstractTwinPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTwinPublisher.class);
    private Multimap<String, StreamObserver<TwinResponseProto>> sinkStreamsByLocation = LinkedListMultimap.create();
    private Map<String, StreamObserver<TwinResponseProto>> sinkStreamsBySystemId = new HashMap<>();
    private final ThreadFactory twinRpcThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("twin-rpc-handler-%d")
            .build();
    private final ExecutorService twinRpcExecutor = Executors.newCachedThreadPool(twinRpcThreadFactory);

    public GrpcTwinPublisher() {
    }

    @Override
    protected void handleSinkUpdate(TwinUpdate sinkUpdate) {
        sendTwinResponseForSink(mapTwinResponse(sinkUpdate));
    }

    private synchronized boolean sendTwinResponseForSink(TwinResponseProto twinResponseProto) {
        if (sinkStreamsByLocation.isEmpty()) {
            return false;
        }
        try {
            if (Strings.isNullOrEmpty(twinResponseProto.getLocation())) {
                LOG.debug("Sending sink update for key {} at all locations", twinResponseProto.getConsumerKey());
                sinkStreamsByLocation.values().forEach(stream -> {
                    stream.onNext(twinResponseProto);
                });
            } else {
                String location = twinResponseProto.getLocation();
                sinkStreamsByLocation.get(location).forEach(stream -> {
                    stream.onNext(twinResponseProto);
                    LOG.debug("Sending sink update for key {} at location {}", twinResponseProto.getConsumerKey(), twinResponseProto.getLocation());
                });
            }
        } catch (Exception e) {
            LOG.error("Error while sending Twin response for Sink stream", e);
        }
        return true;
    }

    public void start() throws IOException {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", GrpcIpcUtils.LOG_PREFIX)) {
            LOG.info("Activated Twin Service");
        }
    }

    public void close() throws IOException {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", GrpcIpcUtils.LOG_PREFIX)) {
            twinRpcExecutor.shutdown();
            LOG.info("Stopped Twin GRPC Server");
        }
    }

    static class AdapterObserver implements StreamObserver<TwinResponseProto> {

        private final Logger logger = LoggerFactory.getLogger(AdapterObserver.class);
        private final StreamObserver<CloudToMinionMessage> delegate;
        private Runnable completionCallback;

        AdapterObserver(StreamObserver<CloudToMinionMessage> delegate) {
            this.delegate = delegate;
        }

        public void setCompletionCallback(Runnable completion) {
            this.completionCallback = completion;
        }

        @Override
        public void onNext(TwinResponseProto value) {
            delegate.onNext(map(value));
        }

        @Override
        public void onError(Throwable t) {
            logger.warn("Error while processing a stream data", t);
        }

        @Override
        public void onCompleted() {
            completionCallback.run();
        }

        private CloudToMinionMessage map(TwinResponseProto value) {
            return CloudToMinionMessage.newBuilder()
                .setTwinResponse(value)
                .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AdapterObserver)) {
                return false;
            }
            AdapterObserver that = (AdapterObserver) o;
            return Objects.equals(delegate, that.delegate) && Objects.equals(
                completionCallback, that.completionCallback);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate, completionCallback);
        }
    }

    // BiConsumer<MinionHeader, StreamObserver<CloudToMinionMessage>>
    public BiConsumer<IpcIdentity, StreamObserver<CloudToMinionMessage>> getStreamObserver() {
        return new BiConsumer<>() {
            @Override
            public void accept(IpcIdentity minionHeader, StreamObserver<CloudToMinionMessage> responseObserver) {
                if (sinkStreamsBySystemId.containsKey(minionHeader.getId())) {
                    StreamObserver<TwinResponseProto> sinkStream = sinkStreamsBySystemId.remove(minionHeader.getId());
                    sinkStreamsByLocation.remove(minionHeader.getLocation(), sinkStream);
                    sinkStream.onCompleted(); // force termination of session.
                }
                AdapterObserver delegate = new AdapterObserver(responseObserver);
                delegate.setCompletionCallback(() -> {
                    sinkStreamsByLocation.remove(minionHeader.getLocation(), delegate);
                    sinkStreamsBySystemId.remove(minionHeader.getId());
                });
                sinkStreamsByLocation.put(minionHeader.getLocation(), delegate);
                sinkStreamsBySystemId.put(minionHeader.getId(), delegate);

                forEachSession(((sessionKey, twinTracker) -> {
                    if (sessionKey.location == null || sessionKey.location.equals(minionHeader.getLocation())) {
                        TwinUpdate twinUpdate = new TwinUpdate(sessionKey.key, sessionKey.location, twinTracker.getObj());
                        twinUpdate.setSessionId(twinTracker.getSessionId());
                        twinUpdate.setVersion(twinTracker.getVersion());
                        twinUpdate.setPatch(false);
                        TwinResponseProto twinResponseProto = mapTwinResponse(twinUpdate);
                        delegate.onNext(twinResponseProto);
                    }
                }));
            }
        };
    }

    // BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>>
    public BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> getRpcObserver() {
        return new BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>>() {
            @Override
            public void accept(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
                if (request.getModuleId().equals("twin")) {
                    try {
                        CompletableFuture.runAsync(() -> {
                            try {
                                TwinRequestProto twinRequest = request.getPayload().unpack(TwinRequestProto.class);
                                TwinUpdate twinUpdate = getTwin(twinRequest);
                                TwinResponseProto twinResponseProto = mapTwinResponse(twinUpdate);
                                LOG.debug("Sent Twin response for key {} at location {}", twinRequest.getConsumerKey(), twinRequest.getLocation());
                                RpcResponseProto rpcResponse = RpcResponseProto.newBuilder()
                                    .setModuleId("twin")
                                    .setRpcId(request.getRpcId())
                                    .setSystemId(request.getSystemId())
                                    .setLocation(request.getLocation())
                                    .setPayload(Any.pack(twinResponseProto))
                                    .build();
                                responseObserver.onNext(rpcResponse);
                            } catch (Exception e) {
                                LOG.error("Exception while processing request", e);
                            }
                        }, twinRpcExecutor);
                    } catch (Exception e) {
                        LOG.error("Could not handle twin rpc request", e);
                    }
                }
            }
        };
    }

}