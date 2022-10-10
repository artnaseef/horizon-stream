package org.opennms.miniongateway.grpc.server.twin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServerBuilder;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.grpc.server.manager.adapter.MinionRSTransportAdapter;
import org.opennms.miniongateway.grpc.server.IncomingRpcHandlerAdapter;
import org.opennms.miniongateway.grpc.server.ServerHandler;

/**
 * This test verifies basic dispatching logic between low level GRPC calls and of higher level APIs such as
 * {@link ServerHandler}.
 *
 * @author ldywicki
 */
@RunWith(MockitoJUnitRunner.class)
public class GrpcInterfaceTest {

    public static final String TEST_MODULE_ID = "test-module";
    public static final String UNKNOWN_MODULE_ID = "unknown-module";
    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private OpennmsGrpcServer server;

    private String name;

    @Mock
    private ServerHandler serverHandler;

    @Before
    public void setUp() throws Exception {
        name = UUID.randomUUID().toString();
        GrpcIpcServerBuilder serverBuilder = new GrpcIpcServerBuilder(new Properties(), 0, "PT0S");
        InProcessServerBuilder test = InProcessServerBuilder.forName(name).directExecutor();
        serverBuilder.setServerBuilder(test);

        when(serverHandler.getId()).thenReturn(TEST_MODULE_ID);

        IncomingRpcHandlerAdapter adapter = new IncomingRpcHandlerAdapter(Arrays.asList(serverHandler));

        server = new OpennmsGrpcServer(serverBuilder);
        server.setTransportAdapter(new MinionRSTransportAdapter(
            null, null,  adapter, null
        ));
        server.start().whenComplete((server, error) -> {
            if (server != null) {
                grpcCleanup.register(server);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testRpcRequest() throws Exception {
        CloudServiceStub stub = createClient();

        RpcRequestProto request = RpcRequestProto.newBuilder().setModuleId(TEST_MODULE_ID).build();
        RpcResponseProto response = RpcResponseProto.newBuilder().build();
        when(serverHandler.handle(request)).thenReturn(
            CompletableFuture.completedFuture(response)
        );

        stub.minionToCloudRPC(request, new GreenPathObserver<>() {
            @Override
            public void onNext(RpcResponseProto rsp) {
                assertEquals(response, rsp);
                verify(serverHandler).handle(request);
            }
        });
    }

    @Test(timeout = 1000L)
    public void testUnknownRpcRequest() throws Exception {
        CloudServiceStub stub = createClient();

        RpcRequestProto request = RpcRequestProto.newBuilder().setModuleId(UNKNOWN_MODULE_ID)
            .setPayload(Any.pack(Struct.newBuilder().build()))
            .build();

        stub.minionToCloudRPC(request, new GreenPathObserver<>() {
            @Override
            public void onNext(RpcResponseProto value) {
                fail("This operation should report error");
            }

            @Override
            public void onError(Throwable error) {
                assertTrue(error instanceof StatusRuntimeException);
            }
        });
    }

    private CloudServiceStub createClient() {
        InProcessChannelBuilder channel = InProcessChannelBuilder.forName(name).directExecutor().maxInboundMessageSize(1024).usePlaintext();
        ManagedChannel managedChannel = channel.build();
        grpcCleanup.register(managedChannel);
        return CloudServiceGrpc.newStub(managedChannel);
    }

    static abstract class GreenPathObserver<T extends Message> implements StreamObserver<T> {

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
        }

        @Override
        public void onCompleted() {

        }
    }
}
