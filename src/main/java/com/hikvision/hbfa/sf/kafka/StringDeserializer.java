package com.hikvision.hbfa.sf.kafka;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringDeserializer implements Deserializer<String> {
    private Charset encoding = StandardCharsets.UTF_8;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String propertyName = isKey ? "key.deserializer.encoding" : "value.deserializer.encoding";
        Object encodingValue = configs.get(propertyName);
        if (encodingValue == null)
            encodingValue = configs.get("deserializer.encoding");
        if (encodingValue instanceof String)
            encoding = Charset.forName((String) encodingValue);
    }

    @Override
    public String deserialize(String topic, byte[] data) {
        if (data == null) return null;
        return new String(data, encoding);
    }
}
