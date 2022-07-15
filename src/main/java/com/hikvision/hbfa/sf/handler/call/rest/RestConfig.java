package com.hikvision.hbfa.sf.handler.call.rest;

import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
public class RestConfig {
    private HttpMethod method;
    private String uri;
    private Map<String, String> headers;
    private Map<String, String> params;
    private Map<String, String> queries;
    private String body;
}
