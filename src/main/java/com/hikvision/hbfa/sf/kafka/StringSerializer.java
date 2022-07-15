package com.hikvision.hbfa.sf.kafka;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringSerializer implements Serializer<String> {
    private Charset encoding = StandardCharsets.UTF_8;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String propertyName = isKey ? "key.serializer.encoding" : "value.serializer.encoding";
        Object encodingValue = configs.get(propertyName);
        if (encodingValue == null)
            encodingValue = configs.get("serializer.encoding");
        if (encodingValue instanceof String)
            encoding = Charset.forName((String) encodingValue);
    }

    @Override
    public byte[] serialize(String topic, String data) {
        if (null == data) return null;
        return data.getBytes(encoding);
    }

}
