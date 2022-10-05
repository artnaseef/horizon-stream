package org.opennms.horizon.shared.ipc.grpc.server.manager.adapter;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageConsumerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinkAdapter extends AbstractMessageConsumerManager implements StreamObserver<MinionToCloudMessage> {

    private final Logger logger = LoggerFactory.getLogger(OpennmsGrpcServer.class);

    private final ThreadFactory sinkConsumerThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("sink-consumer-%d")
        .build();

    private final Map<String, SinkModule<?, Message>> sinkModulesById = new ConcurrentHashMap<>();
    // Maintains the map of sink consumer executor and by module Id.
    private final Map<String, ExecutorService> sinkConsumersByModuleId = new ConcurrentHashMap<>();

    @Override
    protected void startConsumingForModule(SinkModule<?, Message> module) throws Exception {
        if (sinkConsumersByModuleId.get(module.getId()) == null) {
            int numOfThreads = getNumConsumerThreads(module);
            ExecutorService executor = Executors.newFixedThreadPool(numOfThreads, sinkConsumerThreadFactory);
            sinkConsumersByModuleId.put(module.getId(), executor);
            logger.info("Adding {} consumers for module: {}", numOfThreads, module.getId());
        }
        sinkModulesById.putIfAbsent(module.getId(), module);
    }

    @Override
    protected void stopConsumingForModule(SinkModule<?, Message> module) throws Exception {
        ExecutorService executor = sinkConsumersByModuleId.get(module.getId());
        if (executor != null) {
            executor.shutdownNow();
        }
        logger.info("Stopped consumers for module: {}", module.getId());
        sinkModulesById.remove(module.getId());
    }

    @Override
    public void onNext(MinionToCloudMessage message) {
        if (message.hasSinkMessage()) {
            SinkMessage sinkMessage = message.getSinkMessage();
            if (!Strings.isNullOrEmpty(sinkMessage.getModuleId())) {
                ExecutorService sinkModuleExecutor = sinkConsumersByModuleId.get(sinkMessage.getModuleId());
                if (sinkModuleExecutor != null) {
                    sinkModuleExecutor.execute(() -> dispatchSinkMessage(sinkMessage));
                }
            }
        } else {
            logger.error("Unsupported message {}", message);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("Error in sink streaming", throwable);
    }

    @Override
    public void onCompleted() {

    }

    private void dispatchSinkMessage(SinkMessage sinkMessage) {
        SinkModule<?, Message> sinkModule = sinkModulesById.get(sinkMessage.getModuleId());
        if (sinkModule != null && sinkMessage.getContent() != null) {
            Message message = sinkModule.unmarshal(sinkMessage.getContent().toByteArray());
            dispatch(sinkModule, message);
        }
    }

    public void shutdown() {
        sinkModulesById.values().forEach(moduleId -> {
            try {
                stopConsumingForModule(moduleId);
            } catch (Exception e) {
                logger.warn("Error while stopping consumer for module {}", moduleId, e);
            }
        });
        sinkModulesById.clear();
    }

}
