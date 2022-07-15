package com.hikvision.hbfa.sf.dto;

import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class TaskSourceDto {
    @NotEmpty
    private String name;
    @NotEmpty
    private String workflow;
    @NotNull
    private TaskSourceType type;
    private Map<String, Object> config;
    @Min(1)
    private int batch;
    @Min(0)
    private long timeout;
    @Min(0)
    private int retries;

    private boolean autostart;

    private String remark;

    private TaskSourceStatus status;
}
