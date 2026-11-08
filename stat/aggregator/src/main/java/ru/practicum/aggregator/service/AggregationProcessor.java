package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.kafka.AggregatorKafkaClient;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationProcessor {

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final AggregatorKafkaClient kafkaClient;
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    @Value("${stats.kafka.user-actions-topic:stats.user-actions.v1}")
    private String userActionsTopic;

    @Value("${stats.kafka.events-similarity-topic:stats.events-similarity.v1}")
    private String similaritiesTopic;

    public void start() {
        boolean pendingCommit = false;
        try {
            kafkaClient.consumer().subscribe(List.of(userActionsTopic));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<Long, UserActionAvro> records =
                        kafkaClient.consumer().poll(Duration.ofSeconds(1));

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    process(record.value());
                }

                if (!records.isEmpty()) {
                    pendingCommit = true;
                    // Every similarity must reach Kafka before its source offset is committed.
                    kafkaClient.producer().flush();
                    kafkaClient.consumer().commitSync();
                    pendingCommit = false;
                }
            }
        } catch (WakeupException e) {
            log.info("Aggregator consumer stopped");
        } catch (RuntimeException e) {
            log.error("Aggregator processing failed", e);
        } finally {
            try {
                kafkaClient.producer().flush();
                if (pendingCommit) {
                    kafkaClient.consumer().commitSync();
                }
            } catch (RuntimeException e) {
                log.error("Unable to flush or commit aggregator messages during shutdown", e);
            } finally {
                kafkaClient.close();
            }
        }
    }

    void process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        Map<Long, Double> userEvents = userEventWeights.computeIfAbsent(userId, ignored -> new HashMap<>());
        double previousWeight = userEvents.getOrDefault(eventId, 0.0);
        double currentWeight = weight(action.getActionType());

        if (currentWeight <= previousWeight) {
            return;
        }

        userEvents.put(eventId, currentWeight);

        eventWeightSums.merge(eventId, currentWeight - previousWeight, Double::sum);

        // Only events this user actually interacted with can contribute to the numerator.
        for (Map.Entry<Long, Double> otherEvent : userEvents.entrySet()) {
            long otherEventId = otherEvent.getKey();
            if (otherEventId == eventId) {
                continue;
            }
            double otherWeight = otherEvent.getValue();

            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);

            double previousMin = Math.min(previousWeight, otherWeight);
            double currentMin = Math.min(currentWeight, otherWeight);
            double minSum = getMinWeightSum(eventA, eventB)
                    + currentMin - previousMin;

            minWeightSums
                    .computeIfAbsent(eventA, ignored -> new HashMap<>())
                    .put(eventB, minSum);

            double denominator = Math.sqrt(eventWeightSums.get(eventA))
                    * Math.sqrt(eventWeightSums.get(eventB));

            if (denominator == 0.0) {
                continue;
            }

            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(minSum / denominator)
                    .setTimestamp(action.getTimestamp())
                    .build();

            kafkaClient.producer().send(
                    new ProducerRecord<>(similaritiesTopic, eventA, similarity)
            );
        }

    }

    private double getMinWeightSum(long eventA, long eventB) {
        return minWeightSums
                .getOrDefault(eventA, Map.of())
                .getOrDefault(eventB, 0.0);
    }

    private double weight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
