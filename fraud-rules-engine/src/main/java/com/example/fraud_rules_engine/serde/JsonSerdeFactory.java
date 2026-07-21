package com.example.fraud_rules_engine.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JsonSerde;

public class JsonSerdeFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // needed for Instant fields

    public static <T> Serde<T> forType(Class<T> type) {
        JsonSerde<T> serde = new JsonSerde<>(type, OBJECT_MAPPER);
        serde.ignoreTypeHeaders(); // don't require/emit Java type headers — keeps the wire format
        // consumable by non-Java services too, a real interop concern
        return serde;
    }
}
