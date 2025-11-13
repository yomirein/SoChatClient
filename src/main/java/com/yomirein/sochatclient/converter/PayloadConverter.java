package com.yomirein.sochatclient.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
public class PayloadConverter {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static <T> T convertPayload(Object payload, Class<T> clazz) {
        return mapper.convertValue(payload, clazz);
    }
}
