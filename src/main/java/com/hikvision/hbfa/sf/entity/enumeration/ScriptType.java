package com.hikvision.hbfa.sf.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hikvision.hbfa.sf.util.Typed;


public class ScriptType extends Typed {

    private ScriptType(String name) {
        super(name);
    }

    //

    //
    // 预置类型
    //

    public static final ScriptType JMESPath = getOrNew("JMESPath");

    public static final ScriptType QLExpress = getOrNew("QLExpress");


    //

    public static ScriptType getOrNew(String name) {
        return getOrNew0(ScriptType.class, name, ScriptType::new);
    }

    @JsonCreator
    public static ScriptType valueOf(String name) {
        return valueOf(ScriptType.class, name);
    }

}
