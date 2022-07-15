package com.hikvision.hbfa.sf.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.hikvision.hbfa.sf.util.JsonUtil;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.*;

public class JsonDeserializer<T> implements Deserializer<T> {

    private final JavaType javaType;

    public JsonDeserializer(JavaType javaType) {
        this.javaType = Objects.requireNonNull(javaType);
    }

    public JsonDeserializer(Class<T> type) {
        this(JsonUtil.typeFactory().constructType(type));
    }


    @Override
    public T deserialize(String topic, byte[] data) {
        return JsonUtil.fromJson(data, javaType);
    }


    //

    public static <T> JsonDeserializer<? extends Collection<T>>
    forSet(Class<T> elementType) {
        return new JsonDeserializer<>(JsonUtil.typeFactory()
                .constructCollectionType(HashSet.class, elementType));
    }

    public static <T> JsonDeserializer<List<T>>
    forList(Class<T> elementType) {
        return new JsonDeserializer<>(JsonUtil.typeFactory()
                .constructCollectionType(ArrayList.class, elementType));
    }

    public static <K, V> JsonDeserializer<Map<K, V>>
    forMap(Class<K> keyType, Class<V> valueType) {
        return new JsonDeserializer<>(JsonUtil.typeFactory()
                .constructMapType(HashMap.class, keyType, valueType));
    }


}
