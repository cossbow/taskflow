package com.hikvision.hbfa.sf.entity;

import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import lombok.Data;

import java.time.Instant;

@Data
public class TaskSource {
    private long id;
    private String name;
    private String workflow;
    private TaskSourceType type;
    private String config;
    private int batch;
    private long timeout;
    private int retries;

    private boolean autostart;

    private String remark;
    private Instant createdAt;
    private Instant updatedAt;
}
