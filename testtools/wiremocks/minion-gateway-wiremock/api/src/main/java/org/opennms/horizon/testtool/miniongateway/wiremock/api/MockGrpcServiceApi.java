package org.opennms.horizon.testtool.miniongateway.wiremock.api;

import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;

import java.util.List;

public interface MockGrpcServiceApi {
    void sendMessageToLocation(String location, CloudToMinionMessage message);
    void sendMessageToMinion(String minionId, CloudToMinionMessage message);
    List<Identity> getConnectedMinions();
}
