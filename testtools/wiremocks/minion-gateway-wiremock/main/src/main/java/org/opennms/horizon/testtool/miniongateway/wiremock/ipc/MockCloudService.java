package org.opennms.horizon.testtool.miniongateway.wiremock.ipc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockGrpcServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MockCloudService extends CloudServiceGrpc.CloudServiceImplBase implements MockGrpcServiceApi {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MockCloudService.class);

    private Logger log = DEFAULT_LOGGER;

    private Map<String, List<StreamObserver<CloudToMinionMessage>>> cloudToLocationStreamMap = new ConcurrentHashMap<>();
    private Map<String, AtomicInteger> locationSequenceMap = new ConcurrentHashMap<>();
    private Map<String, StreamObserver<CloudToMinionMessage>> cloudToMinionStreamMap = new ConcurrentHashMap<>();
    private Set<Identity> connectedMinions = Collections.synchronizedSet(new HashSet<>());

//========================================
// INTERFACE
//----------------------------------------

    @Override
    public void sendMessageToLocation(String location, CloudToMinionMessage message) {
        var streamObserverList = cloudToLocationStreamMap.get(location);
        if ((streamObserverList != null) && (! streamObserverList.isEmpty())) {
            locationSequenceMap.computeIfAbsent(location, key -> new AtomicInteger(0));
            int seq = locationSequenceMap.get(location).getAndIncrement();

            var streamObserver= streamObserverList.get(seq % streamObserverList.size());
            streamObserver.onNext(message);
        } else {
            throw new RuntimeException("don't have a connection for location " + location);
        }
    }

    @Override
    public void sendMessageToMinion(String minionId, CloudToMinionMessage message) {
        StreamObserver<CloudToMinionMessage> streamObserver = cloudToMinionStreamMap.get(minionId);
        if (streamObserver != null) {
            streamObserver.onNext(message);
        } else {
            throw new RuntimeException("don't have a connection for Minion " + minionId);
        }
    }

    @Override
    public List<Identity> getConnectedMinions() {
        return new LinkedList<>(connectedMinions);
    }

//========================================
// GRPC Service Endpoints
//----------------------------------------

    @Override
    public void cloudToMinionMessages(Identity minionIdentity, StreamObserver<CloudToMinionMessage> streamObserver) {
        log.info(
            "Have cloud-to-minion stream connection request: location={}; system-id={}",
            minionIdentity.getLocation(),
            minionIdentity.getSystemId());

        connectedMinions.add(minionIdentity);

        cloudToMinionStreamMap.put(minionIdentity.getSystemId(), streamObserver);
        cloudToLocationStreamMap.compute(minionIdentity.getLocation(),
            (key, list) -> {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(streamObserver);

                return list;
            });
    }

    @Override
    public StreamObserver<MinionToCloudMessage> minionToCloudMessages(StreamObserver<Empty> responseObserver) {
        log.info("Have minion-to-cloud message initiated");

        return new StreamObserver<>() {
            @Override
            public void onNext(MinionToCloudMessage value) {
                log.info("Have minion-to-cloud-message: twin-request.consumer-key={}", value.getTwinRequest().getConsumerKey());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }
}
