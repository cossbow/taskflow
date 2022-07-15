package com.hikvision.hbfa.sf.dto;

import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Data
public class TaskStartDto {
    // 工作流名称
    @NotEmpty
    private String name;
    // 输入参数
    private ObjectMap input;
    // 任务超时时间，毫秒
    @Min(0)
    private long timeout;
    // 返回各节点的output
    private boolean verbose;
}
