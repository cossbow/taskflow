package com.hikvision.hbfa.sf.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class SubtaskDto {
    private Object id;
    private long nodeId;
    private String nodeName;

    private boolean success;
    private Map<String, Object> output;
    private String errMsg;
    private int retries;

    private Instant createdAt;
    private Instant updatedAt;

}
