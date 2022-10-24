package org.opennms.horizon.testtool.miniongateway.wiremock.ipc;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockCloudServiceConfiguration {
    @Value("${mock.cloud-service.hostname:0.0.0.0}")
    @Getter
    private String hostname;

    @Value("${mock.cloud-service.port:8990}")
    @Getter
    private int port;

    @Value("${mock.cloud-service.maxMessageSize:10485760}")
    @Getter
    private int maxMessageSize;
}
