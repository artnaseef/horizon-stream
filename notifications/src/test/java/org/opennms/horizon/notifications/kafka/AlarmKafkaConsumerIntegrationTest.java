package org.opennms.horizon.notifications.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(topics = {
    "${horizon.kafka.alarms.topic}",
})
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class AlarmKafkaConsumerIntegrationTest {
    private static final int KAFKA_TIMEOUT = 5000;

    @Value("${horizon.kafka.alarms.topic}")
    private String alarmsTopic;

    private Producer<String, AlarmDTO> alarmProducer;
    private Producer<String, String> stringProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @SpyBean
    private AlarmKafkaConsumer alarmKafkaConsumer;

    @Captor
    ArgumentCaptor<AlarmDTO> alarmCaptor;

    @BeforeAll
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        DefaultKafkaProducerFactory<String, AlarmDTO> alarmFactory
            = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new JsonSerializer<>());
        alarmProducer = alarmFactory.createProducer();

        DefaultKafkaProducerFactory<String, String> stringFactory
            = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new StringSerializer());
        stringProducer = stringFactory.createProducer();
    }

    @Disabled
    @Test
    void testAlarmKafkaConsumer() {
        AlarmDTO alarm = new AlarmDTO();
        alarm.setId(7654321);

        alarmProducer.send(new ProducerRecord<>(alarmsTopic, alarm));
        alarmProducer.flush();

        verify(alarmKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alarmCaptor.capture());

        AlarmDTO capturedAlarm = alarmCaptor.getValue();
        assertEquals(alarm.getId(), capturedAlarm.getId());

        alarmProducer.close();
    }

    @Test
    void testStringKafkaConsumer() {
        int id = 1234;
        stringProducer.send(new ProducerRecord<>(alarmsTopic, String.format("{\"id\": %d, \"severity\":\"indeterminate\", \"logMessage\":\"hello\"}", id)));
        stringProducer.flush();

        verify(alarmKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alarmCaptor.capture());

        AlarmDTO capturedAlarm = alarmCaptor.getValue();
        assertEquals(id, capturedAlarm.getId());

        stringProducer.close();
    }

    @AfterAll
    void shutdown() {
        alarmProducer.close();
        stringProducer.close();
    }
}
