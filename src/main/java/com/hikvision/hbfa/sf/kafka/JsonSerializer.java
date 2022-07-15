package com.hikvision.hbfa.sf.kafka;

import com.hikvision.hbfa.sf.util.JsonUtil;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (null == data) return null;
        return JsonUtil.toJsonBytes(data);
    }

}
