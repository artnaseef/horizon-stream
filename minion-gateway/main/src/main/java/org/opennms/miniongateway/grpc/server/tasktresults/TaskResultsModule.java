package org.opennms.miniongateway.grpc.server.tasktresults;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.taskset.contract.TaskSetResults;

public class TaskResultsModule implements SinkModule<Message, Message> {

    public static final String MODULE_ID = "task-set-result";

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(Message message) {
        return message.toByteArray();
    }

    @Override
    public Message unmarshal(byte[] content) {
        try {
            return TaskSetResults.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(Message message) {
        return marshal(message);
    }

    @Override
    public Message unmarshalSingleMessage(byte[] message) {
        return unmarshal(message);
    }

    @Override
    public AggregationPolicy<Message, Message, ?> getAggregationPolicy() {
        return new IdentityAggregationPolicy<>();
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return 10;
            }

            @Override
            public int getNumThreads() {
                return 1;
            }

            @Override
            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }
}
