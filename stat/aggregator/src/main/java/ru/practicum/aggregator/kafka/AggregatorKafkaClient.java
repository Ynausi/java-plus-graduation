package ru.practicum.aggregator.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Component
public class AggregatorKafkaClient {

    private final Consumer<Long, UserActionAvro> consumer;
    private final Producer<Long, EventSimilarityAvro> producer;

    public AggregatorKafkaClient(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${stats.kafka.aggregator-group:aggregator}") String groupId) {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName()
        );
        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                UserActionAvroDeserializer.class.getName()
        );
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumer = new KafkaConsumer<>(consumerProperties);

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName()
        );
        producerProperties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                EventSimilarityAvroSerializer.class.getName()
        );
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 3);
        producer = new KafkaProducer<>(producerProperties);
    }

    public Consumer<Long, UserActionAvro> consumer() {
        return consumer;
    }

    public Producer<Long, EventSimilarityAvro> producer() {
        return producer;
    }

    @PreDestroy
    public void close() {
        consumer.wakeup();
        producer.close();
    }
}
