package ru.practicum.analyzer.kafka;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityAvroDeserializer
        implements Deserializer<EventSimilarityAvro> {

    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<EventSimilarityAvro> reader =
                    new SpecificDatumReader<>(EventSimilarityAvro.class);
            return reader.read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize event similarity", e);
        }
    }
}
