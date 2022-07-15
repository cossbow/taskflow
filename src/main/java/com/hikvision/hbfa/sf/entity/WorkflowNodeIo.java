package com.hikvision.hbfa.sf.entity;

import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import lombok.Data;

import java.time.Instant;

@Data
public class WorkflowNodeIo {
    private long workflowId;
    private long nodeId;

    // 使用jsonpath来传参
    private ObjectMap inputParameters;

    private boolean ignoreError;

    private Instant createdAt;
    private Instant updatedAt;
}
