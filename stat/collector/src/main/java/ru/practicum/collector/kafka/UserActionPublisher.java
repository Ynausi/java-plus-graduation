package ru.practicum.collector.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class UserActionPublisher {

    private final Producer<Long, UserActionAvro> producer;
    private final String topic;

    public UserActionPublisher(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${stats.kafka.user-actions-topic:stats.user-actions.v1}") String topic) {
        this.topic = topic;

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                UserActionAvroSerializer.class.getName()
        );
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        this.producer = new KafkaProducer<>(properties);
    }

    public void publish(UserActionAvro action) {
        try {
            // A successful gRPC response means Kafka acknowledged the action.
            producer.send(new ProducerRecord<>(topic, action.getUserId(), action))
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing a user action", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaException("Failed to publish a user action to Kafka", e);
        }
    }

    @PreDestroy
    public void close() {
        producer.close();
    }
}
