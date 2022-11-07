package org.opennms.miniongateway.rpcrequest;

import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

import java.util.concurrent.CompletableFuture;

public interface RpcRequestRouter {
    CompletableFuture<RpcResponseProto> routeRequest(RpcRequestProto request);
}
