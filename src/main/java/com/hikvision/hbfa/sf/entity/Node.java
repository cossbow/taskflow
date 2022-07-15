package com.hikvision.hbfa.sf.entity;

import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.entity.json.SubmitConfig;
import lombok.Data;

import java.time.Instant;

@Data
public class Node {
    private long id;
    // 唯一名称
    private String name;
    // 类型，对应不同的处理handler
    private NodeType type;
    // 配置，给不同的处理handler使用
    private String config;
    // 默认实参：可以被实际参数覆盖
    private ObjectMap defaultArguments;

    // 是否异步提交
    private boolean async;
    // 提交配置
    private SubmitConfig submitConfig;

    // 重试次数。
    private int maxRetries;
    // 备注说明
    private String remark;

    private Instant createdAt;
    private Instant updatedAt;




}
