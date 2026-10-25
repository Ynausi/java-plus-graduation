package ru.practicum.analyzer.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
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
        executor.submit(this::consumeActions);
        executor.submit(this::consumeSimilarities);
    }

    private void consumeActions() {
        actionConsumer.subscribe(List.of(userActionsTopic));
        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records =
                        actionConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    saveAction(record.value());
                }
                if (!records.isEmpty()) {
                    actionConsumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
            log.info("User action consumer stopped");
        }
    }

    private void consumeSimilarities() {
        similarityConsumer.subscribe(List.of(similaritiesTopic));
        try {
            while (running) {
                ConsumerRecords<Long, EventSimilarityAvro> records =
                        similarityConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    saveSimilarity(record.value());
                }
                if (!records.isEmpty()) {
                    similarityConsumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
            log.info("Event similarity consumer stopped");
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
        executor.shutdownNow();
        actionConsumer.close();
        similarityConsumer.close();
    }
}
