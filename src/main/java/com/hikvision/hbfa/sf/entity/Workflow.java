package com.hikvision.hbfa.sf.entity;

import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import lombok.Data;

import java.time.Instant;

@Data
public class Workflow {
    private long id;
    private String name;

    // 使用jsonpath来传参
    private ObjectMap outputParameters;
    // 完成时通知
    private CallType notifier;
    private String notifierConfig;  // notifier 不为空时需要

    // 超时时间
    private long timeout;

    private String remark;
    private Instant createdAt;
    private Instant updatedAt;
}
