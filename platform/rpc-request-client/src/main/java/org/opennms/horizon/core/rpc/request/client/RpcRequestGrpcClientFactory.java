package org.opennms.horizon.core.rpc.request.client;

import com.google.protobuf.Message;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;

public class RpcRequestGrpcClientFactory<T extends Message> implements RpcClientFactory {

    public static final String DEFAULT_GRPC_HOSTNAME = "localhost";
    public static final int DEFAULT_GRPC_PORT = 8990;
    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1_0485_760;

    private boolean tlsEnabled = false;
    private String host = DEFAULT_GRPC_HOSTNAME;
    private int port = DEFAULT_GRPC_PORT;
    private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

//========================================
// Getters and Setters
//----------------------------------------

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

//========================================
// RpcClientFactory INTERFACE
//----------------------------------------

    @Override
    public <T extends Message> RpcClient<T> getClient(Deserializer<T> deserializer) {
        RpcRequestGrpcClient<T> rpcRequestGrpcClient = new RpcRequestGrpcClient<>();

        rpcRequestGrpcClient.setDeserializer(deserializer);

        rpcRequestGrpcClient.setHost(host);
        rpcRequestGrpcClient.setPort(port);
        rpcRequestGrpcClient.setTlsEnabled(tlsEnabled);
        rpcRequestGrpcClient.setMaxMessageSize(maxMessageSize);

        rpcRequestGrpcClient.init();

        return rpcRequestGrpcClient;
    }

    @Override
    public RpcClient<RpcResponseProto> getClient() {
        return getClient(response -> response);
    }
}
