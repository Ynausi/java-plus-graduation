package ru.practicum.aggregator.service;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.aggregator.kafka.AggregatorKafkaClient;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregationProcessorTest {

    @Mock
    private AggregatorKafkaClient kafkaClient;
    @Mock
    private Producer<Long, EventSimilarityAvro> producer;

    @Test
    void onlyEventsInteractedWithBySameUserContributeToSimilarity() {
        AggregationProcessor processor = new AggregationProcessor(kafkaClient);
        ReflectionTestUtils.setField(processor, "similaritiesTopic", "test-similarities");
        when(kafkaClient.producer()).thenReturn(producer);

        processor.process(action(5L, 100L, ActionTypeAvro.VIEW));
        processor.process(action(5L, 101L, ActionTypeAvro.REGISTER));
        processor.process(action(6L, 102L, ActionTypeAvro.VIEW));

        ArgumentCaptor<ProducerRecord> record = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer, times(1)).send(record.capture());
        EventSimilarityAvro similarity = (EventSimilarityAvro) record.getValue().value();
        assertEquals(100L, similarity.getEventA());
        assertEquals(101L, similarity.getEventB());
        assertEquals(0.4 / Math.sqrt(0.4 * 0.8), similarity.getScore(), 1e-9);
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro actionType) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
