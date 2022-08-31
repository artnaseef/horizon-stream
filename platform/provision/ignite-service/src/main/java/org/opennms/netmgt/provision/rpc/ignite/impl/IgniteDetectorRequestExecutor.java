package org.opennms.netmgt.provision.rpc.ignite.impl;

import io.opentracing.Span;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;
import org.opennms.horizon.shared.ignite.remoteasync.manager.IgniteRemoteAsyncManager;
import org.opennms.miniongateway.detector.client.IgniteDetectorRemoteOperation;
import org.opennms.netmgt.provision.DetectorRequestExecutor;
import org.opennms.netmgt.provision.PreDetectCallback;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IgniteDetectorRequestExecutor implements DetectorRequestExecutor {

    private final Ignite ignite;
    private final String location;
    private final String systemId;
    private final String serviceName;
    private final String detectorName;
    private final InetAddress address;
    private final Map<String, String> attributes;
    private final Integer nodeId;
    private final Span span; // TODO: wire into the remote call
    private final PreDetectCallback preDetectCallback; // TODO: what does this do?  If needed, wire it across ignite

    private final IgniteRemoteAsyncManager igniteRemoteAsyncManager;
    private final DetectorRequestRouteManager detectorRequestRouteManager;

    public IgniteDetectorRequestExecutor(
        Ignite ignite,
        String location,
        String systemId,
        String serviceName,
        String detectorName,
        InetAddress address,
        Map<String, String> attributes,
        Integer nodeId,
        Span span,
        PreDetectCallback preDetectCallback,
        IgniteRemoteAsyncManager igniteRemoteAsyncManager,
        DetectorRequestRouteManager detectorRequestRouteManager
    ) {

        this.ignite = ignite;
        this.location = location;
        this.systemId = systemId;
        this.serviceName = serviceName;
        this.detectorName = detectorName;
        this.address = address;
        this.attributes = attributes;
        this.nodeId = nodeId;
        this.span = span;
        this.preDetectCallback = preDetectCallback;
        this.igniteRemoteAsyncManager = igniteRemoteAsyncManager;
        this.detectorRequestRouteManager = detectorRequestRouteManager;
    }

    @Override
    public CompletableFuture<Boolean> execute() {

        UUID nodeId = findNodeIdToUse();

        if (nodeId == null) {
            return CompletableFuture.failedFuture(
                new Exception("cannot (currently) reach a minion at location=" + location + ", system-id=" + systemId));
        }

        IgniteDetectorRemoteOperation remoteOperation = prepareRemoteOperation();

        ClusterGroup clusterGroup = ignite.cluster().forNodeId(nodeId);
        CompletableFuture future = igniteRemoteAsyncManager.submit(clusterGroup, remoteOperation);

        return future;
    }

    /**
     * Determine which Node ID to use for the next execution.
     *
     * @return
     */
    private UUID findNodeIdToUse() {
        // If system-id was specified, send downstream to that system only
        if (systemId != null) {
            return detectorRequestRouteManager.findNodeIdToUseForSystemId(systemId);
        } else {
            return detectorRequestRouteManager.findNodeIdToUseForLocation(location);
        }
    }

    private IgniteDetectorRemoteOperation prepareRemoteOperation() {
        IgniteDetectorRemoteOperation result = new IgniteDetectorRemoteOperation();
        result.setLocation(location);
        result.setSystemId(systemId);
        result.setServiceName(serviceName);
        result.setDetectorName(detectorName);
        result.setAddress(address);
        result.setAttributes(attributes);
        result.setNodeId(nodeId);

        return result;
    }
}
