package com.hikvision.hbfa.sf.handler.script;

import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import io.burt.jmespath.JmesPathException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class JMESPathScriptExecutor implements ScriptExecutor {
    @Override
    public ScriptType type() {
        return ScriptType.JMESPath;
    }

    @Override
    public Predicate<Map<String, Object>> createCondition(String script) {
        var expression = ParameterUtil.JCF_RUNTIME.compile(script);
        return map -> {
            var v = expression.search(map);
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            throw new IllegalArgumentException("script must return bool");
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<Map<String, Object>, Map<String, Object>> createFunction(String script) {
        var expression = ParameterUtil.JCF_RUNTIME.compile(script);
        return map -> {
            Object output;
            try {
                output = expression.search(map);
            } catch (JmesPathException e) {
                throw new IllegalArgumentException(
                        "script run error: " + script + ", data is: " + JsonUtil.lazyJson(map), e);
            }
            if (output instanceof Map) {
                return (Map<String, Object>) output;
            }
            throw new IllegalArgumentException("script must return map: " + script);
        };
    }
}
