package com.hikvision.hbfa.sf.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hikvision.hbfa.sf.util.Typed;

public class CallType extends Typed {

    private CallType(String name) {
        super(name);
    }

    //

    //
    // 预置类型
    //

    public static final CallType REST = getOrNew("REST");

    public static final CallType KAFKA = getOrNew("KAFKA");


    //

    public static CallType getOrNew(String name) {
        return getOrNew0(CallType.class, name, CallType::new);
    }

    @JsonCreator
    public static CallType valueOf(String name) {
        return valueOf(CallType.class, name);
    }

}
