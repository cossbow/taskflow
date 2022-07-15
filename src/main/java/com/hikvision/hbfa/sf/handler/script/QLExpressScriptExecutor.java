package com.hikvision.hbfa.sf.handler.script;

import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import com.hikvision.hbfa.sf.util.Slf4j2LoggingAdapter;
import com.ql.util.express.*;
import com.ql.util.express.exception.QLException;
import com.ql.util.express.instruction.OperateDataCacheManager;
import com.ql.util.express.instruction.op.OperatorBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
public class QLExpressScriptExecutor implements ScriptExecutor {
    private static final Log L = new Slf4j2LoggingAdapter(log);


    private final ExpressRunner runner;

    public QLExpressScriptExecutor() {
        runner = new ExpressRunner();
        runner.addFunction("json", new JsonOperator());
        runner.addFunction("dejson", new DeJsonOperator());
        runner.addFunction("jsonpath", new JsonPathOperator());
    }

    @Override
    public ScriptType type() {
        return ScriptType.QLExpress;
    }

    private InstructionSet parse(String script) {
        try {
            return runner.parseInstructionSet(script);
        } catch (Exception e) {
            throw new IllegalArgumentException("syntax error", e);
        }
    }

    private Object execute(InstructionSet instructionSet, Map<String, Object> data) {
        try {
            return runner.execute(instructionSet, new MapExpressContext<>(data),
                    null, false, false, L);
        } catch (Exception e) {
            throw new IllegalArgumentException("script execute error", e);
        }
    }

    @Override
    public Predicate<Map<String, Object>> createCondition(String script) {
        var instructionSet = parse(script);
        return data -> {
            Object result = execute(instructionSet, data);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new IllegalArgumentException("script must return bool");
        };
    }


    @SuppressWarnings("unchecked")
    @Override
    public Function<Map<String, Object>, Map<String, Object>> createFunction(String script) {
        var instructionSet = parse(script);
        return data -> {
            var output = execute(instructionSet, data);
            if (output instanceof Map) {
                return (Map<String, Object>) output;
            }
            throw new IllegalArgumentException("script must return map");
        };
    }


    private static class MapExpressContext<K, V> implements IExpressContext<K, V> {
        private final Map<K, V> data;

        MapExpressContext(Map<K, V> data) {
            this.data = data;
        }

        @Override
        public V get(Object key) {
            return data.get(key);
        }

        @Override
        public V put(K name, V object) {
            return data.put(name, object);
        }
    }


    //


    private static class JsonPathOperator extends OperatorBase {

        static DefaultContext<String, Object> topContext(IExpressContext<String, Object> child) {
            if (child instanceof InstructionSetContext) {
                return topContext(((InstructionSetContext) child).getParent());
            } else if (child instanceof DefaultContext) {
                return (DefaultContext<String, Object>) child;
            }
            return null;
        }

        @Override
        public OperateData executeInner(InstructionSetContext parent, ArraySwap list) throws Exception {
            var context = topContext(parent);
            if (null == context) {
                return OperateDataCacheManager.fetchOperateData(null, null);
            }
            if (list.length < 1) {
                throw new QLException("JsonPath required one string parameter.");
            }
            var arg = list.get(0).getObject(parent);
            if (!(arg instanceof String)) {
                throw new QLException("JsonPath required one string parameter.");
            }

            var path = (String) arg;
            var def = list.length > 1 ? list.get(1).getObject(parent) : null;
            Object result = ParameterUtil.readParamByNudeJsonpath(context, path);
            if (null == result) result = def;
            if (null == result) {
                return OperateDataCacheManager.fetchOperateData(null, null);
            }
            return OperateDataCacheManager.fetchOperateData(result,
                    ExpressUtil.getSimpleDataType(result.getClass()));
        }

    }

    private static class JsonOperator extends Operator {
        public Object executeInner(Object[] list) throws Exception {
            if (list.length == 0) {
                throw new QLException("json operator required one parameter");
            }
            var v = list[0];
            if (null == v) return null;
            return JsonUtil.toJson(v);
        }
    }

    private static class DeJsonOperator extends Operator {
        public Object executeInner(Object[] list) throws Exception {
            if (list.length == 0) {
                throw new QLException("dejson operator required one parameter");
            }
            var v = list[0];
            if (null == v) return null;
            if (v instanceof String) {
                try {
                    return JsonUtil.fromJson((String) v, Object.class);
                } catch (IllegalArgumentException e) {
                    throw new QLException(e.getMessage(), e);
                }
            }
            throw new QLException("dejson operator required json string but input: " + v);
        }
    }


}
