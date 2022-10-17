package org.opennms.horizon.testtool.miniongateway.wiremock.api;

public interface MockTwinHandler {
    void publish(String location, String topic, byte[] content);
}
