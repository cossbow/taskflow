package com.hikvision.hbfa.sf.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class TaskDto {
    private Object id;
    private long workflowId;
    private String workflowName;

    private Map<String, Object> input;
    private boolean success;
    private Map<String, Object> output;
    private String errMsg;

    private Instant createdAt;
    private Instant updatedAt;

    private List<SubtaskDto> subtasks;

}
