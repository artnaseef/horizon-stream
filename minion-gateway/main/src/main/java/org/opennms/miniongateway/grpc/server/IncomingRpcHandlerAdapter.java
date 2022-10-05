package org.opennms.miniongateway.grpc.server;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class IncomingRpcHandlerAdapter implements BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> {

    private Map<String, ServerHandler> handlers;

    public IncomingRpcHandlerAdapter(List<ServerHandler> handlers) {
        this.handlers = handlers.stream()
            .collect(Collectors.toMap(ServerHandler::getId, Function.identity()));
    }

    @Override
    public void accept(RpcRequestProto request, StreamObserver<RpcResponseProto> responseStream) {
        if (handlers.containsKey(request.getModuleId())) {
            ServerHandler handler = handlers.get(request.getModuleId());
            handler.handle(request).whenComplete((response, error) -> {
                if (error != null) {
                    responseStream.onError(new StatusException(Status.INTERNAL.withCause(error).withDescription("Could not handle request")));
                    return;
                }
                responseStream.onNext(response);
                responseStream.onCompleted();
            });
        } else {
            log.warn("Request for unsupported module {}", request.getModuleId());
            responseStream.onError(new StatusException(Status.INVALID_ARGUMENT.withDescription("Requested module is not known")));
        }
    }
}
