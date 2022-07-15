package com.hikvision.hbfa.sf.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class WorkflowNodeEdge {
    private long workflowId;
    private long fromNodeId;
    private long toNodeId;

    private Instant createdAt;
}
