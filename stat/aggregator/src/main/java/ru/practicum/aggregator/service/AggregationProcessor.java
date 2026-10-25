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
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    @Value("${stats.kafka.user-actions-topic:stats.user-actions.v1}")
    private String userActionsTopic;

    @Value("${stats.kafka.events-similarity-topic:stats.events-similarity.v1}")
    private String similaritiesTopic;

    public void start() {
        kafkaClient.consumer().subscribe(List.of(userActionsTopic));

        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<Long, UserActionAvro> records =
                        kafkaClient.consumer().poll(Duration.ofSeconds(1));

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    process(record.value());
                }

                if (!records.isEmpty()) {
                    kafkaClient.consumer().commitSync();
                }
            }
        } catch (WakeupException e) {
            log.info("Aggregator consumer stopped");
        }
    }

    void process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double previousWeight = getUserWeight(eventId, userId);
        double currentWeight = weight(action.getActionType());

        if (currentWeight <= previousWeight) {
            return;
        }

        eventUserWeights
                .computeIfAbsent(eventId, ignored -> new HashMap<>())
                .put(userId, currentWeight);

        eventWeightSums.merge(eventId, currentWeight - previousWeight, Double::sum);

        for (Long otherEventId : List.copyOf(eventWeightSums.keySet())) {
            if (otherEventId == eventId) {
                continue;
            }

            double otherWeight = getUserWeight(otherEventId, userId);
            if (otherWeight == 0.0) {
                continue;
            }

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

        kafkaClient.producer().flush();
    }

    private double getUserWeight(long eventId, long userId) {
        return eventUserWeights
                .getOrDefault(eventId, Map.of())
                .getOrDefault(userId, 0.0);
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
