package com.hikvision.hbfa.sf.handler.script;

import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.util.TypedBean;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ScriptExecutor extends TypedBean<ScriptType> {

    Predicate<Map<String, Object>> createCondition(String script);

    Function<Map<String, Object>, Map<String, Object>> createFunction(String script);

}
