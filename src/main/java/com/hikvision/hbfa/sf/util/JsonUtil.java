package com.hikvision.hbfa.sf.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;

final
public class JsonUtil {
    private JsonUtil() {
    }


    static final JsonMapper jsonMapper;

    static {
        var module = new SimpleModule();
        // 时间相关
        module.addSerializer(Instant.class, serializer(DateTimeFormatter.ISO_INSTANT::format));
        module.addDeserializer(Instant.class, deserializer(Instant::parse));
        module.addSerializer(OffsetDateTime.class, serializer(DatetimeUtil::iso8601));
        module.addDeserializer(OffsetDateTime.class, deserializer(DatetimeUtil::iso8601));
        module.addSerializer(Duration.class, DurationSerializer.INSTANCE);
        module.addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
        // 字符串接口
        module.addSerializer(CharSequence.class, ToStringSerializer.instance);
        module.addDeserializer(CharSequence.class, StringDeserializer.instance);
        // 构造
        jsonMapper = JsonMapper.builder()
                .addModule(module)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public static <T> JsonSerializer<T> serializer(Function<T, String> translator) {
        return new JsonSerializer<>() {
            @Override
            public void serialize(T v, JsonGenerator g, SerializerProvider p) throws IOException {
                g.writeString(translator.apply(v));
            }
        };
    }

    public static <T> JsonDeserializer<T> deserializer(Function<String, T> translator) {
        return new JsonDeserializer<>() {
            @Override
            public T deserialize(JsonParser p, DeserializationContext c) throws IOException {
                return translator.apply(p.getValueAsString());
            }
        };
    }

    public static TypeFactory typeFactory() {
        return jsonMapper.getTypeFactory();
    }

    public static <T> JavaType typeOf(Class<T> type) {
        return typeFactory().constructType(type);
    }

    public static <T> T fromJson(String j, Class<T> type) {
        try {
            return jsonMapper.readValue(j, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static <T> T fromJson(byte[] j, Class<T> type) {
        try {
            return jsonMapper.readValue(j, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static <T> T fromJson(String j, JavaType type) {
        try {
            return jsonMapper.readValue(j, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static <T> T fromJson(byte[] j, JavaType type) {
        try {
            return jsonMapper.readValue(j, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static String toJson(Object o) {
        try {
            return jsonMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serial json error", e);
        }
    }

    public static byte[] toJsonBytes(Object o) {
        try {
            return jsonMapper.writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serial json error", e);
        }
    }

    public static void writeJson(DataOutput out, Object o) throws IOException {
        try {
            jsonMapper.writeValue(out, o);
        } catch (JsonGenerationException | JsonMappingException e) {
            throw new IllegalStateException("serial json error", e);
        }
    }

    public static <T> T readJson(DataInput in, Class<T> type) throws IOException {
        try {
            return jsonMapper.readValue(in, type);
        } catch (JsonParseException | JsonMappingException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static <T> T readJson(DataInput in, JavaType type) throws IOException {
        try {
            return jsonMapper.readValue(in, type);
        } catch (JsonParseException | JsonMappingException e) {
            throw new IllegalArgumentException("invalid json or de-serial error", e);
        }
    }

    public static ObjectMap parseJsonMap(String s) {
        if (null == s) return null;
        return fromJson(s, ObjectMap.class);
    }

    public static <T> CacheLoader<String, T> cacheLoader(Class<T> type) {
        Objects.requireNonNull(type);
        return s -> fromJson(s, type);
    }

    public static CharSequence lazyJson(Object o) {
        if (null == o) return "null";
        return LazyToString.of(() -> toJson(o));
    }

}
