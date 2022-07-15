package com.hikvision.hbfa.sf.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hikvision.hbfa.sf.util.Typed;

import java.util.Collection;

public class NodeType extends Typed {

    private final CallType callType;

    private NodeType(String name, CallType callType) {
        super(name);
        this.callType = callType;
    }

    public CallType callType() {
        return callType;
    }

    //

    //
    // 预置类型
    //

    // Restful调用的任务
    public static final NodeType REST = getOrNew("REST", CallType.REST);
    // 发送到Kafka的任务
    public static final NodeType KAFKA = getOrNew("KAFKA", CallType.KAFKA);
    // subflow子流程，内嵌workflow
    public static final NodeType SUBFLOW = getOrNew("SUBFLOW");
    // 脚本任务
    public static final NodeType SCRIPT = getOrNew("SCRIPT");
    // 批量执行任务
    public static final NodeType BATCH = getOrNew("BATCH");
    // 决策任务
    public static final NodeType DECISION = getOrNew("DECISION");
    // 音频任务：特殊任务
    public static final NodeType VOICE_ANALYSIS = getOrNew("VOICE_ANALYSIS");

    //

    public static NodeType getOrNew(String name) {
        return getOrNew(name, null);
    }

    public static NodeType getOrNew(String name, CallType callType) {
        return getOrNew0(NodeType.class, name, n -> new NodeType(n, callType));
    }

    @JsonCreator
    public static NodeType valueOf(String name) {
        return valueOf(NodeType.class, name);
    }

    public static Collection<NodeType> values() {
        return values(NodeType.class);
    }
}
