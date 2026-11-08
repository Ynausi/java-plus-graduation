package ru.practicum.analyzer.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserInteraction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AnalyzerKafkaConsumer {

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final UserInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;
    private final Consumer<Long, UserActionAvro> actionConsumer;
    private final Consumer<Long, EventSimilarityAvro> similarityConsumer;
    private final String userActionsTopic;
    private final String similaritiesTopic;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean running = true;

    public AnalyzerKafkaConsumer(
            UserInteractionRepository interactionRepository,
            EventSimilarityRepository similarityRepository,
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${stats.kafka.user-actions-topic:stats.user-actions.v1}") String userActionsTopic,
            @Value("${stats.kafka.events-similarity-topic:stats.events-similarity.v1}") String similaritiesTopic) {
        this.interactionRepository = interactionRepository;
        this.similarityRepository = similarityRepository;
        this.userActionsTopic = userActionsTopic;
        this.similaritiesTopic = similaritiesTopic;
        this.actionConsumer = createActionConsumer(bootstrapServers);
        this.similarityConsumer = createSimilarityConsumer(bootstrapServers);
    }

    @PostConstruct
    public void start() {
        executor.execute(() -> consume(actionConsumer, userActionsTopic, this::saveAction));
        executor.execute(() -> consume(similarityConsumer, similaritiesTopic, this::saveSimilarity));
    }

    private <T> void consume(Consumer<Long, T> consumer,
                             String topic,
                             java.util.function.Consumer<T> processor) {
        try {
            consumer.subscribe(List.of(topic));
            while (running) {
                try {
                    ConsumerRecords<Long, T> records = consumer.poll(Duration.ofSeconds(1));
                    if (records.isEmpty()) {
                        continue;
                    }

                    boolean processed = true;
                    for (ConsumerRecord<Long, T> record : records) {
                        try {
                            processor.accept(record.value());
                        } catch (RuntimeException e) {
                            log.error("Failed to save Kafka record at {}:{}:{}; retrying batch",
                                    record.topic(), record.partition(), record.offset(), e);
                            processed = false;
                            break;
                        }
                    }

                    if (processed) {
                        consumer.commitSync();
                    } else {
                        // The poll advanced every partition. Rewind all of them so no records are lost.
                        for (TopicPartition partition : records.partitions()) {
                            consumer.seek(partition, records.records(partition).get(0).offset());
                        }
                        backoff();
                    }
                } catch (RecordDeserializationException e) {
                    // A corrupt payload cannot be processed. Log its exact offset before skipping it.
                    log.error("Skipping invalid Kafka record at {}:{}", e.topicPartition(), e.offset(), e);
                    consumer.seek(e.topicPartition(), e.offset() + 1);
                    consumer.commitSync();
                } catch (WakeupException e) {
                    if (running) {
                        log.warn("Unexpected wakeup on Kafka topic {}", topic, e);
                    } else {
                        break;
                    }
                } catch (RuntimeException e) {
                    log.error("Kafka consumer for {} failed; will retry", topic, e);
                    backoff();
                }
            }
        } catch (RuntimeException e) {
            log.error("Kafka consumer for {} terminated unexpectedly", topic, e);
        } finally {
            // KafkaConsumer is not thread-safe: only its owning worker closes it.
            try {
                consumer.close();
            } catch (RuntimeException e) {
                log.error("Unable to close Kafka consumer for {}", topic, e);
            }
            log.info("Kafka consumer for {} stopped", topic);
        }
    }

    private void backoff() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private void saveAction(UserActionAvro action) {
        double newWeight = weight(action.getActionType());

        UserInteraction interaction = interactionRepository
                .findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .orElseGet(() -> UserInteraction.builder()
                        .userId(action.getUserId())
                        .eventId(action.getEventId())
                        .weight(0.0)
                        .build());

        interaction.setWeight(Math.max(interaction.getWeight(), newWeight));
        interaction.setUpdatedAt(action.getTimestamp());
        interactionRepository.save(interaction);
    }

    private void saveSimilarity(EventSimilarityAvro message) {
        long eventA = Math.min(message.getEventA(), message.getEventB());
        long eventB = Math.max(message.getEventA(), message.getEventB());

        EventSimilarity similarity = similarityRepository
                .findByEventAAndEventB(eventA, eventB)
                .orElseGet(() -> EventSimilarity.builder()
                        .eventA(eventA)
                        .eventB(eventB)
                        .build());

        similarity.setScore(message.getScore());
        similarity.setUpdatedAt(message.getTimestamp());
        similarityRepository.save(similarity);
    }

    private Consumer<Long, UserActionAvro> createActionConsumer(String bootstrapServers) {
        Properties properties = commonConsumerProperties(
                bootstrapServers,
                "analyzer-user-actions"
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                UserActionAvroDeserializer.class.getName()
        );
        return new KafkaConsumer<>(properties);
    }

    private Consumer<Long, EventSimilarityAvro> createSimilarityConsumer(
            String bootstrapServers) {
        Properties properties = commonConsumerProperties(
                bootstrapServers,
                "analyzer-event-similarities"
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                EventSimilarityAvroDeserializer.class.getName()
        );
        return new KafkaConsumer<>(properties);
    }

    private Properties commonConsumerProperties(String bootstrapServers, String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName()
        );
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return properties;
    }

    private double weight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    @PreDestroy
    public void close() {
        running = false;
        actionConsumer.wakeup();
        similarityConsumer.wakeup();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Analyzer Kafka consumers did not stop within 30 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while awaiting Kafka consumer shutdown", e);
        }
    }
}
