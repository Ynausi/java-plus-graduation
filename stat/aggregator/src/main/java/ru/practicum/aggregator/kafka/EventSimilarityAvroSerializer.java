package ru.practicum.aggregator.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EventSimilarityAvroSerializer implements Serializer<EventSimilarityAvro> {

    @Override
    public byte[] serialize(String topic, EventSimilarityAvro data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            SpecificDatumWriter<EventSimilarityAvro> writer =
                    new SpecificDatumWriter<>(EventSimilarityAvro.class);
            writer.write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize event similarity", e);
        }
    }
}
