package com.hikvision.hbfa.sf.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hikvision.hbfa.sf.util.Typed;


public class TaskSourceType extends Typed {

    private TaskSourceType(String name) {
        super(name);
    }


    //

    //
    // 预置类型
    //

    public static final TaskSourceType KAFKA = getOrNew("KAFKA");


    //

    public static TaskSourceType getOrNew(String name) {
        return getOrNew0(TaskSourceType.class, name, TaskSourceType::new);
    }

    @JsonCreator
    public static TaskSourceType valueOf(String name) {
        return valueOf(TaskSourceType.class, name);
    }

}
