package com.hikvision.hbfa.sf.handler.call;

import lombok.Data;

import java.util.Map;

@Data
public class CallParam<Config> {
    private final String key;
    private final Config config;
    private final Map<String, Object> input;
}
