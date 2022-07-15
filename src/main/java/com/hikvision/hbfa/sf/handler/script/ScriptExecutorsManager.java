package com.hikvision.hbfa.sf.handler.script;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.util.TypedBeanManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class ScriptExecutorsManager extends TypedBeanManager<ScriptType, ScriptExecutor> {


    private final Map<String, Predicate<Map<String, Object>>> predicateCache
            = new ConcurrentHashMap<>();

    private final Map<Long, Function<Map<String, Object>, Map<String, Object>>> functionCache
            = new ConcurrentHashMap<>();

    private final LoadingCache<Script, Predicate<Map<String, Object>>>
            lruPredicateCache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(this::createCondition);
    private final LoadingCache<Script, Function<Map<String, Object>, Map<String, Object>>>
            lruFunctionCache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(this::createFunction);

    public ScriptExecutorsManager(List<ScriptExecutor> executors) {
        super("ScriptExecutors", executors);
    }


    private Predicate<Map<String, Object>> createCondition(Script condition) {
        return get(condition.getType()).createCondition(condition.getText());
    }

    private Function<Map<String, Object>, Map<String, Object>> createFunction(Script script) {
        return get(script.getType()).createFunction(script.getText());
    }

    private static String keyOf(long nodeId, long workflowId) {
        return nodeId + ":" + workflowId;
    }


    public void delConditionCache(long nodeId, long workflowId) {
        predicateCache.remove(keyOf(nodeId, workflowId));
    }

    /**
     * 获取一个条件对象，用于测试子流程是否可以启动
     */
    public Optional<Predicate<Map<String, Object>>> getCondition(long nodeId, long workflowId, Script condition) {
        if (null == condition) {
            return Optional.empty();
        }
        var key = keyOf(nodeId, workflowId);
        return Optional.of(
                predicateCache.computeIfAbsent(key, k -> createCondition(condition))
        );
    }

    /**
     * 测试一个脚本，如果有错误则抛出异常
     *
     * @param script 待测试的脚本
     * @throws IllegalArgumentException 脚本有错误
     */
    public boolean checkConditionSyntax(Script script, Map<String, Object> env)
            throws IllegalArgumentException {
        return createCondition(script).test(env);
    }


    /**
     * 获取一个函数
     *
     * @param nodeId 节点id
     * @param script 输入脚本
     */
    public Function<Map<String, Object>, Map<String, Object>>
    getFunction(long nodeId, Script script) {
        return functionCache.computeIfAbsent(nodeId, id -> createFunction(script));
    }

    public Map<String, Object> checkFunctionSyntax(Script script, Map<String, Object> env)
            throws IllegalArgumentException {
        return createFunction(script).apply(env);
    }

    public void delFunctionCache(long nodeId) {
        functionCache.remove(nodeId);
    }


    //

    public Predicate<Map<String, Object>> getCacheCondition(Script script) {
        return lruPredicateCache.get(script);
    }

    public Function<Map<String, Object>, Map<String, Object>>
    getCachedFunction(Script script) {
        return lruFunctionCache.get(script);
    }

}
