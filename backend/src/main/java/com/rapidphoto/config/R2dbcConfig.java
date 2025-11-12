package com.rapidphoto.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R2DBC configuration for custom type conversions.
 * Handles Map<String, Object> to JSONB conversion for PostgreSQL.
 */
@Configuration
public class R2dbcConfig {

    private final ObjectMapper objectMapper;

    public R2dbcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new MapToJsonConverter(objectMapper));
        converters.add(new JsonToMapConverter(objectMapper));
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    /**
     * Converts Map<String, Object> to PostgreSQL JSONB for writing.
     */
    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {

        private final ObjectMapper objectMapper;

        public MapToJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(Map<String, Object> source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                return Json.of(json);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to convert Map to JSON", e);
            }
        }
    }

    /**
     * Converts PostgreSQL JSONB to Map<String, Object> for reading.
     */
    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {

        private final ObjectMapper objectMapper;

        public JsonToMapConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Map<String, Object> convert(Json source) {
            try {
                return objectMapper.readValue(source.asString(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // If parsing fails, return empty map rather than throwing
                return new HashMap<>();
            }
        }
    }
}
