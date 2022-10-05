package org.opennms.miniongateway.grpc.server.twin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceImplBase;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServerBuilder;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.miniongateway.grpc.server.ConnectionIdentity;
import org.opennms.miniongateway.grpc.server.IncomingRpcHandlerAdapter;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinPublisher.Session;
import org.opennms.miniongateway.grpc.twin.TwinRpcHandler;

public class TwinPublisherTest {

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private OpennmsGrpcServer server;

    private String name;
    private Session<Map> session;

    @Before
    public void setUp() throws Exception {
        name = UUID.randomUUID().toString();
        GrpcIpcServerBuilder serverBuilder = new GrpcIpcServerBuilder(new Properties(), 0, "PT0S");
        InProcessServerBuilder test = InProcessServerBuilder.forName(name).directExecutor();
        serverBuilder.setServerBuilder(test);

        GrpcTwinPublisher publisher = new GrpcTwinPublisher();
        session = publisher.register("DigitalTwin", Map.class, "foo");
        session.publish(ImmutableMap.of("key", "value"));

        IncomingRpcHandlerAdapter adapter = new IncomingRpcHandlerAdapter(Arrays.asList(new TwinRpcHandler(publisher)));

        server = new OpennmsGrpcServer(serverBuilder);
        server.setTransportAdapter(new CloudServiceImplBase() {
            @Override
            public void minionToCloudRPC(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
                adapter.accept(request, responseObserver);
            }

            @Override
            public void cloudToMinionMessages(Identity request, StreamObserver<CloudToMinionMessage> responseObserver) {
                IpcIdentity minionId = new ConnectionIdentity(request);
                publisher.getStreamObserver().accept(minionId, responseObserver);
            }
        });
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

    @Test(timeout = 1000L)
    public void testDigitalTwinRequest() throws Exception {
        CloudServiceStub stub = createClient();

        RpcRequestProto request = RpcRequestProto.newBuilder().setModuleId("twin")
            .setPayload(Any.pack(
                TwinRequestProto.newBuilder()
                    .setLocation("foo").setSystemId("bar")
                    .setConsumerKey("DigitalTwin")
                    .build()
            ))
            .build();

        stub.minionToCloudRPC(request, new GreenPathObserver<>() {
            @Override
            public void onNext(RpcResponseProto response) {
                try {
                    assertTrue(response.getPayload().is(TwinResponseProto.class));
                    TwinResponseProto twinResponse = response.getPayload().unpack(TwinResponseProto.class);
                    assertFalse(twinResponse.getIsPatchObject());
                    assertEquals("{\"key\":\"value\"}", twinResponse.getTwinObject().toStringUtf8());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test(timeout = 1000L)
    public void testUnknownTwinRequest() throws Exception {
        CloudServiceStub stub = createClient();

        RpcRequestProto request = RpcRequestProto.newBuilder().setModuleId("twin")
            .setPayload(Any.pack(
                TwinRequestProto.newBuilder()
                    .setLocation("foo").setSystemId("bar")
                    .setConsumerKey("Mystery")
                    .build()
            ))
            .build();

        stub.minionToCloudRPC(request, new GreenPathObserver<>() {
            @Override
            public void onNext(RpcResponseProto response) {
                try {
                    assertTrue(response.getPayload().is(TwinResponseProto.class));
                    TwinResponseProto twinResponse = response.getPayload().unpack(TwinResponseProto.class);
                    assertEquals("", twinResponse.getTwinObject().toStringUtf8());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test(timeout = 1000L)
    public void testTwinRequestAndUpdate() throws Exception {
        CloudServiceStub stub = createClient();

        RpcRequestProto request = RpcRequestProto.newBuilder().setModuleId("twin")
            .setPayload(Any.pack(
                TwinRequestProto.newBuilder()
                    .setLocation("foo").setSystemId("bar")
                    .setConsumerKey("DigitalTwin")
                    .build()
            ))
            .build();

        CountDownLatch rpcAnswer = new CountDownLatch(1);
        CountDownLatch messageConsumed = new CountDownLatch(1);

        stub.minionToCloudRPC(request, new GreenPathObserver<>() {
            @Override
            public void onNext(RpcResponseProto response) {
                try {
                    rpcAnswer.countDown();
                    assertTrue(response.getPayload().is(TwinResponseProto.class));
                    TwinResponseProto twinResponse = response.getPayload().unpack(TwinResponseProto.class);
                    assertFalse(twinResponse.getIsPatchObject());
                    assertEquals("{\"key\":\"value\"}", twinResponse.getTwinObject().toStringUtf8());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        rpcAnswer.await();
        session.publish(ImmutableMap.of("key", "value", "key2", "value2"));

        // subscribe alter for update
        stub.cloudToMinionMessages(Identity.newBuilder().setLocation("foo").setSystemId("bar").build(), new GreenPathObserver<>() {
            @Override
            public void onNext(CloudToMinionMessage message) {
                System.out.println("Received message " + message);
                TwinResponseProto twinResponse = message.getTwinResponse();
                assertFalse(twinResponse.getIsPatchObject());
                assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", twinResponse.getTwinObject().toStringUtf8());
                assertEquals(1, twinResponse.getVersion());
                messageConsumed.countDown();
            }
        });

        messageConsumed.await();
    }

    @Test(timeout = 1000L)
    public void testTwinSubscription() throws Exception {
        CloudServiceStub stub = createClient();

        CountDownLatch firstMessage = new CountDownLatch(1);
        CountDownLatch secondMessage = new CountDownLatch(1);

        // subscribe alter for update
        stub.cloudToMinionMessages(Identity.newBuilder().setLocation("foo").setSystemId("bar").build(), new GreenPathObserver<>() {
            @Override
            public void onNext(CloudToMinionMessage message) {
                TwinResponseProto twinResponse = message.getTwinResponse();
                if (firstMessage.getCount() == 1) {
                    assertFalse(twinResponse.getIsPatchObject());
                    assertEquals("{\"key\":\"value\"}", twinResponse.getTwinObject().toStringUtf8());
                    assertEquals(0, twinResponse.getVersion());
                    firstMessage.countDown();
                    return;
                }

                assertEquals("[{\"op\":\"add\",\"path\":\"/key2\",\"value\":\"value2\"}]", twinResponse.getTwinObject().toStringUtf8());
                assertEquals(1, twinResponse.getVersion());
                secondMessage.countDown();
            }
        });

        firstMessage.await();
        session.publish(ImmutableMap.of("key", "value", "key2", "value2"));
        secondMessage.await();
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
        }

        @Override
        public void onCompleted() {

        }
    }
}
