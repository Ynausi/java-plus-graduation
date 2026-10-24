package ru.practicum.aggregator.kafka;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<UserActionAvro> reader =
                    new SpecificDatumReader<>(UserActionAvro.class);
            return reader.read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize user action", e);
        }
    }
}
