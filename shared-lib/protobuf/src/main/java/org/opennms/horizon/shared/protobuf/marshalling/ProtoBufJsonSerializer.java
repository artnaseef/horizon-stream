package org.opennms.horizon.shared.protobuf.marshalling;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.Method;

public class ProtoBufJsonSerializer<T extends Message> extends JsonSerializer<T> {
    Class clazz;

    public ProtoBufJsonSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> handledType() {
        return clazz;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            Method getDescriptorMethod = clazz.getMethod("getDescriptor");
            Descriptors.Descriptor descriptor = (Descriptors.Descriptor) getDescriptorMethod.invoke(null);

            TypeRegistry typeRegistry =
                TypeRegistry.newBuilder()
                    .add(descriptor)
                    .build();

            gen.writeRaw(JsonFormat.printer().usingTypeRegistry(typeRegistry).print(value));
        } catch (Exception exc) {
            throw new RuntimeException("failed to serialize protobuf message: type-name=" + clazz.getTypeName(), exc);
        }
    }
}
