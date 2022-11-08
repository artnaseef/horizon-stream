package org.opennms.miniongateway.rpcrequest.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.miniongateway.rpcrequest.RpcRequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Component
public class RpcRequestGrpcService extends RpcRequestServiceGrpc.RpcRequestServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestGrpcService.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private RpcRequestRouter rpcRequestRouter;

    @Autowired
    private GrpcIpcServer grpcIpcServer;

    //========================================
    // Lifecycle
    //----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        grpcIpcServer.startServer(this);
        log.info("Started RPC-Request GRPC Service");
    }

    //========================================
    // Service API
    //----------------------------------------

    @Override
    public void request(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
        CompletableFuture<RpcResponseProto> future = rpcRequestRouter.routeRequest(request);

        future.whenComplete(
            (response, exception) -> handleCompletedRequest(response, exception, responseObserver)
        );
    }

    //========================================
    // Internals
    //----------------------------------------

    private void handleCompletedRequest(RpcResponseProto response, Throwable exception, StreamObserver<RpcResponseProto> responseObserver) {
        if (exception != null) {
            Status status = Status.UNAVAILABLE.withDescription(exception.getMessage());
            responseObserver.onError(status.asRuntimeException());
        } else {
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }
}
