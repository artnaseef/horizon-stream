package org.opennms.miniongateway.grpc.server.twin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinPublisher.Session;
import org.opennms.miniongateway.grpc.twin.TwinUpdate;

/**
 * Verification of higher level logic of {@link org.opennms.miniongateway.grpc.twin.TwinPublisher}.
 *
 * This test rely on hooks found in default implementation provided by {@link org.opennms.miniongateway.grpc.twin.AbstractTwinPublisher}
 * and bridged by {@link GrpcTwinPublisher} for later use.
 *
 */
public class TwinPublisherTest {

    public static final String LOCATION = "foo";
    public static final String SYSTEM = "bar";
    public static final String DIGITAL_TWIN_CONSUMER_KEY = "DigitalTwin";
    public static final String DIGITAL_TWIN_UNKNOWN_CONSUMER_KEY = "Mystery";
    private Session<Map> session;
    private GrpcTwinPublisher publisher;

    private TwinUpdate twinUpdate;

    @Before
    public void setUp() throws Exception {
        publisher = new GrpcTwinPublisher() {
            @Override
            protected void handleSinkUpdate(TwinUpdate sinkUpdate) {
                super.handleSinkUpdate(sinkUpdate);
                TwinPublisherTest.this.twinUpdate = sinkUpdate;
            }
        };
        session = publisher.register(DIGITAL_TWIN_CONSUMER_KEY, Map.class, LOCATION);
        session.publish(ImmutableMap.of("key", "value"));
    }

    @After
    public void tearDown() throws Exception {
        session.close();
        publisher.close();
    }

    @Test
    public void testDigitalTwinRequest() throws Exception {
        TwinRequestProto request = TwinRequestProto.newBuilder()
            .setLocation(LOCATION).setSystemId(SYSTEM)
            .setConsumerKey(DIGITAL_TWIN_CONSUMER_KEY)
            .build();

        TwinResponseProto response = publisher.getTwinResponse(request);
        assertFalse(response.getIsPatchObject());
        assertEquals("{\"key\":\"value\"}", response.getTwinObject().toStringUtf8());
        assertEquals("{\"key\":\"value\"}", new String(twinUpdate.getObject()));
    }

    @Test
    public void testUnknownTwinRequest() throws Exception {
        TwinRequestProto request = TwinRequestProto.newBuilder()
            .setLocation(LOCATION).setSystemId(SYSTEM)
            .setConsumerKey(DIGITAL_TWIN_UNKNOWN_CONSUMER_KEY)
            .build();

        TwinResponseProto twinResponse = publisher.getTwinResponse(request);
        assertEquals("", twinResponse.getTwinObject().toStringUtf8());
    }

    @Test
    public void testTwinRequestAndUpdate() throws Exception {
        TwinRequestProto request = TwinRequestProto.newBuilder()
            .setLocation(LOCATION).setSystemId(SYSTEM)
            .setConsumerKey(DIGITAL_TWIN_CONSUMER_KEY)
            .build();

        TwinResponseProto twinResponse = publisher.getTwinResponse(request);
        assertFalse(twinResponse.getIsPatchObject());
        assertEquals("{\"key\":\"value\"}", twinResponse.getTwinObject().toStringUtf8());
        assertEquals("{\"key\":\"value\"}", new String(twinUpdate.getObject()));

        session.publish(ImmutableMap.of("key", "value", "key2", "value2"));

        // subscribe alter for update
        twinResponse = publisher.getTwinResponse(request);
        assertFalse(twinResponse.getIsPatchObject());
        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", twinResponse.getTwinObject().toStringUtf8());
        assertEquals(1, twinResponse.getVersion());
        assertEquals("[{\"op\":\"add\",\"path\":\"/key2\",\"value\":\"value2\"}]", new String(twinUpdate.getObject()));
    }

    @Test
    public void testTwinSubscription() throws Exception {
        TwinRequestProto request = TwinRequestProto.newBuilder()
            .setLocation(LOCATION).setSystemId(SYSTEM)
            .setConsumerKey(DIGITAL_TWIN_CONSUMER_KEY)
            .build();

        TwinResponseProto twinResponse = publisher.getTwinResponse(request);
        assertFalse(twinResponse.getIsPatchObject());
        assertEquals("{\"key\":\"value\"}", twinResponse.getTwinObject().toStringUtf8());
        assertEquals(0, twinResponse.getVersion());
        assertEquals("{\"key\":\"value\"}", new String(twinUpdate.getObject()));

        session.publish(ImmutableMap.of("key", "value", "key2", "value2"));
        twinResponse = publisher.getTwinResponse(request);
        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", twinResponse.getTwinObject().toStringUtf8());
        assertEquals(1, twinResponse.getVersion());
        assertEquals("[{\"op\":\"add\",\"path\":\"/key2\",\"value\":\"value2\"}]", new String(twinUpdate.getObject()));
    }

}
