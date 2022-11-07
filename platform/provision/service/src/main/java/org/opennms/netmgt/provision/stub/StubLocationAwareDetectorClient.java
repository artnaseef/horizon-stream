package org.opennms.netmgt.provision.stub;

import org.opennms.netmgt.provision.DetectorRequestExecutorBuilder;
import org.opennms.netmgt.provision.LocationAwareDetectorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary implementation of the LocationAwareDetectorClient.
 *
 * Added to stub-out the LocationAwareDetectorClient to easily enable removal of the Ignite implementation, which is
 * being removed in favor of the GRPC implementation.  Detectors and provisioning in core are not functional with the
 * new minions at this time, so no there is loss of functionality in the end-to-end solution.
 */
@Deprecated
public class StubLocationAwareDetectorClient implements LocationAwareDetectorClient {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(StubLocationAwareDetectorClient.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public DetectorRequestExecutorBuilder detect() {
        log.error("STUB location-aware-detector-client called; expect a NullPointerException");

        return null;
    }
}
