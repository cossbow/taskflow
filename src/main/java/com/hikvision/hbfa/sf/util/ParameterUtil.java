package com.hikvision.hbfa.sf.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.burt.jmespath.*;
import io.burt.jmespath.function.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.burt.jmespath.JmesPathType.*;

/**
 * <h2>参数注入工具</h2>
 *
 * <h3>参数获取方式是JsonPath相似的JMESPath</h3>
 * <a href="https://jmespath.org">JMESPath官网</a>
 * <a href="https://github.com/burtcorp/jmespath-java">Java实现</a>
 */
@Slf4j
final
public class ParameterUtil {
    private ParameterUtil() {
    }

    //

    public static final Predicate<String> NAME_CHECKER =
            Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{2,511}").asMatchPredicate();

    private static final Set<String> ILLEGAL_NAMES;
    private static final LazySingleton<String> ILLEGAL_NAME_TIPS;

    static {
        var s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        s.addAll(List.of("node", "workflow", "task",
                "id", "input", "output", "config",
                "subtask", "flow", "edge", "service", "server",
                "taskId", "subtaskId"));
        ILLEGAL_NAMES = Collections.unmodifiableSet(s);
        ILLEGAL_NAME_TIPS = LazySingleton.of(() -> "illegal names: " + s);
    }

    /**
     * 用于检查名称是否包含非法字符串
     */
    public static void illegalName(String n) {
        if (ILLEGAL_NAMES.contains(n)) {
            throw new IllegalArgumentException(ILLEGAL_NAME_TIPS.get());
        }
        if (!NAME_CHECKER.test(n)) {
            throw new IllegalArgumentException("名称必须字母开头并且由字母数字下划线组成，长度3-512");
        }
    }


    //
    //

    private static final Map<String, Function> FUNCTIONS;
    private static final FunctionRegistry FUNCTION_REGISTRY;

    static {
        Function[] functions = {
                new AbsFunction(),
                new AvgFunction(),
                new ContainsFunction(),
                new CeilFunction(),
                new EndsWithFunction(),
                new FloorFunction(),
                new JoinFunction(),
                new KeysFunction(),
                new LengthFunction(),
                new MapFunction(),
                new MaxFunction(),
                new MaxByFunction(),
                new MergeFunction(),
                new MinFunction(),
                new MinByFunction(),
                new NotNullFunction(),
                new ReverseFunction(),
                new SortFunction(),
                new SortByFunction(),
                new StartsWithFunction(),
                new SumFunction(),
                new ToArrayFunction(),
                new ToStringFunction(),
                new ToNumberFunction(),
                new TypeFunction(),
                new ValuesFunction(),
                new EmptyFunction(),
                new JsonFunction(),
                new DejsonFunction(),
                new ConcatFunction(),
                new PutsFunction(),
                new DefFunction()
        };
        var m = new HashMap<String, Function>(functions.length);
        for (Function f : functions) {
            m.put(f.name(), f);
        }
        FUNCTIONS = Map.copyOf(m);

        FUNCTION_REGISTRY = new FunctionRegistry(functions);
    }

    public static final JmesPath<Object> JCF_RUNTIME =
            new ParamRuntime(RuntimeConfiguration.builder()
                    .withFunctionRegistry(FUNCTION_REGISTRY)
                    .build());
    private static final LoadingCache<String, Expression<Object>> EXPRESSION_CACHE =
            Caffeine.newBuilder().softValues()
                    .expireAfterAccess(Duration.ofMinutes(15))
                    .build(JCF_RUNTIME::compile);

    public static Set<String> supportFunctions() {
        return FUNCTIONS.keySet();
    }

    public static Expression<Object> compile(String script) {
        return JCF_RUNTIME.compile(script);
    }

    public static Expression<Object> getExpression(String script) {
        try {
            var e = EXPRESSION_CACHE.get(script);
            assert null != e;
            return e;
        } catch (JmesPathException e) {
            log.error("Compile JsonPath error: {}", script, e);
            throw new IllegalArgumentException(e);
        }
    }

    private static void invalidateExpressionCache(String script) {
        EXPRESSION_CACHE.invalidate(script);
    }

    //

    // *** 参数引用文档应当与下面的关键key一致 *** //
    // 参数引用标识：中间的是jsonpath
    public static final String REF_PREFIX = "${";               // 参数引用的左边界
    public static final String REF_SUFFIX = "}";                // 参数引用的右边界
    // key前缀
    public static final String KEY_TASK = "task";               // 工作流任务配置属性
    public static final String KEY_SUBTASK = "subtask";               // 工作流任务配置属性
    // key第二段
    public static final String KEY_ID = "id";                   // id
    public static final String KEY_NAME = "name";                   // id
    public static final String KEY_TAG = "tag";                 // tag
    public static final String KEY_INPUT = "input";             // 输入参数
    public static final String KEY_OUTPUT = "output";           // 输出参数
    public static final String KEY_ERROR = "error";             // 输出参数
    // key其他参数
    public static final String KEY_SUBMIT = "submit";           // 提交参数
    // *** -参数引用key *** //

    //

    public static Map<String, Object> argumentMap(long id, Map<String, Object> output) {
        return Map.of(
                KEY_ID, id,
                KEY_OUTPUT, MapUtil.nonnull(output)
        );
    }

    public static Map<String, Object> argumentMap(long id, Map<String, Object> output, String error) {
        if (null == error) return argumentMap(id, output);
        return Map.of(
                KEY_ID, id,
                KEY_OUTPUT, MapUtil.nonnull(output),
                KEY_ERROR, error
        );
    }

    public static Map<String, Object> argumentMap(Map<String, Object> output, String error) {
        if (null == error)
            return Map.of(KEY_OUTPUT, MapUtil.nonnull(output));

        return Map.of(
                KEY_OUTPUT, MapUtil.nonnull(output),
                KEY_ERROR, error
        );
    }


    //

    /**
     * @param arguments
     * @param nudePath
     * @return
     * @throws IllegalArgumentException 表达式执行错误
     */
    public static Object testJsonpath(Map<String, ?> arguments, String nudePath) {
        try {
            return compile(nudePath).search(arguments);
        } catch (JmesPathException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Object readParamByJsonpath(String param, Map<String, ?> args) {
        if (null == param || param.isEmpty()) return param;
        int len = param.length();
        if (len <= 3) return param;
        if (!param.startsWith(REF_PREFIX) || !param.endsWith(REF_SUFFIX)) {
            return param;
        }

        var path = param.substring(2, len - 1);
        return getExpression(path).search(args);
    }

    /**
     * @param arguments 实参
     * @param nudePath  裸的jsonpath，没有被${}包括
     */
    public static Object readParamByNudeJsonpath(Map<String, ?> arguments, String nudePath) {
        return getExpression(nudePath).search(arguments);
    }

    /**
     * 读取单个参数
     *
     * @param arguments 实参
     * @param parameter 形参，含有jsonpath，和注入表达式
     * @return 获取的对象
     */
    public static Object readParam(Map<String, ?> arguments, String parameter) {
        return readParamByJsonpath(parameter, arguments);
    }

    @SuppressWarnings("unchecked")
    public static <T> T readParam(Map<String, ?> arguments, String parameter, Class<T> expect) {
        var o = readParamByJsonpath(parameter, arguments);
        if (o == null) return null;
        if (o.getClass() == expect) {
            return (T) o;
        }
        return null;
    }

    /**
     * 顺序读取多个值
     *
     * @param arguments  实参数据源
     * @param parameters 形参
     * @return 实参，数组length等于形参length，可能有null
     */
    public static Object[] readParams(Map<String, ?> arguments, String... parameters) {
        var values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (null != parameters[i])
                values[i] = readParamByJsonpath(parameters[i], arguments);
        }
        return values;
    }

    public static <M extends Map<String, Object>> M replaceAll(
            Map<String, ?> parameters, Map<String, ?> arguments,
            IntFunction<M> creator) {
        if (null == parameters || parameters.isEmpty()) return creator.apply(0);
        return replaceAllParams(parameters, (arguments), creator);
    }


    /**
     * 替换注入参数
     *
     * @param parameters 形参
     * @param arguments  实参数据源
     * @return 实参
     */
    public static Map<String, Object> replaceAllParams(
            Map<String, ?> parameters, Map<String, Object> arguments) {
        if (null == parameters) return arguments;
        if (parameters.isEmpty()) return Map.of();
        return replaceAll(parameters, arguments, HashMap::new);
    }

    public static <M extends Map<String, Object>> M replaceAllParams(
            Map<String, ?> parameters, Map<String, ?> arguments,
            IntFunction<M> creator) {
        var dst = creator.apply(parameters.size());
        for (var entry : parameters.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (null == key || null == value) continue;

            dst.put(key, checkAndReplaceParam(value, arguments, creator));
        }
        return dst;
    }

    static <M extends Map<String, Object>> Object checkAndReplaceParam(
            Object value, Map<String, ?> arguments,
            IntFunction<M> creator) {
        if (value instanceof String) {
            return readParamByJsonpath((String) value, arguments);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            var subMap = (Map<String, Object>) value;
            if (subMap.size() > 0) {
                return replaceAllParams(subMap, arguments, creator);
            }
        } else if (value instanceof Collection) {
            var c = (Collection<?>) value;
            var l = new ArrayList<>(c.size());
            for (Object o : c) {
                l.add(checkAndReplaceParam(o, arguments, creator));
            }
            return l;
        } else if (value instanceof Object[]) {
            var a = (Object[]) value;
            var l = new ArrayList<>(a.length);
            for (Object o : a) {
                l.add(checkAndReplaceParam(o, arguments, creator));
            }
            return l;
        }
        return value;
    }

    //

    public static final String VERSION = "/v1";
    public static final String TASK_PATH = VERSION + "/task";
    public static final String SUBMIT_PATH = "/submit/";

    public static final String TOPIC_TASK_START = "hicd.service-flow.topic.task-start";
    public static final String TOPIC_NODE_PREFIX = "hicd.service-flow.topic.node-";

    public static CharSequence submitPath(String contextPath, Object subtaskId) {
        return LazyToString.of(() -> contextPath + TASK_PATH + SUBMIT_PATH + subtaskId);
    }

    public static CharSequence topic4Node(long nodeId) {
        return LazyToString.of(() -> TOPIC_NODE_PREFIX + nodeId);
    }


    //

    private static class DefFunction extends BaseFunction {

        public DefFunction() {
            super("def",
                    ArgumentConstraints.anyValue(),
                    ArgumentConstraints.anyValue());
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var value = arguments.get(0).value();
            var def = arguments.get(1).value();
            return null != value ? value : def;
        }
    }

    /**
     * 返回value是否为空
     */
    private static class EmptyFunction extends BaseFunction {
        public EmptyFunction() {
            super("empty",
                    ArgumentConstraints.typeOf(ARRAY, OBJECT, STRING, NULL));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var value = arguments.get(0).value();
            if (value instanceof Collection) {
                return runtime.createBoolean(((Collection<?>) value).isEmpty());
            } else if (value instanceof Map) {
                return runtime.createBoolean(((Map<?, ?>) value).isEmpty());
            } else if (value instanceof String) {
                return runtime.createBoolean(((String) value).isEmpty());
            }
            return runtime.createBoolean(value == null);
        }
    }

    /**
     * 将value转换成json字符串
     */
    private static class JsonFunction extends BaseFunction {
        public JsonFunction() {
            super("json",
                    ArgumentConstraints.typeOf(JmesPathType.values()));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var value = arguments.get(0).value();
            return runtime.createString(JsonUtil.toJson(value));
        }
    }

    /**
     * 将json字符串转换成value
     */
    private static class DejsonFunction extends BaseFunction {
        public DejsonFunction() {
            super("dejson",
                    ArgumentConstraints.typeOf(STRING, NULL));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var value = arguments.get(0).value();
            if (null == value) return null;
            return runtime.parseString(runtime.toString(value));
        }
    }

    /**
     * join字符串，如果为null则输出''
     */
    private static class JoinFunction extends BaseFunction {
        public JoinFunction() {
            super("join",
                    ArgumentConstraints.typeOf(STRING, NULL),
                    new MixArray(new TypeMulti(STRING, NULL))
            );
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            T glue = arguments.get(0).value();
            T components = arguments.get(1).value();
            var list = runtime.toList(components);
            if (list.isEmpty()) return runtime.createString("");

            String glueString = null != glue ? runtime.toString(glue) : "";
            return runtime.createString(list.stream()
                    .map(v -> null != v ? runtime.toString(v) : "")
                    .collect(Collectors.joining(glueString)));
        }
    }

    static class SumFunction extends ArrayMathFunction {
        public SumFunction() {
            super(ArgumentConstraints.typeOf(NUMBER, NULL));
        }

        @Override
        protected <T> T performMathOperation(Adapter<T> runtime, List<T> values) {
            double sum = 0;
            for (T n : values) {
                if (null == n) continue;
                sum += runtime.toNumber(n).doubleValue();
            }
            return runtime.createNumber(sum);
        }
    }


    /**
     * 连接多个array
     */
    private static class ConcatFunction extends BaseFunction {
        public ConcatFunction() {
            super("concat", ArgumentConstraints.listOf(
                    1, Integer.MAX_VALUE,
                    ArgumentConstraints.typeOf(ARRAY, NULL)));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var list = new ArrayList<T>();
            for (var argument : arguments) {
                var v = argument.value();
                if (null == v) continue;
                list.addAll(runtime.toList(v));
            }
            return runtime.createArray(list);
        }
    }

    /**
     * 将指定的key和value放进数组中的每个对象里
     */
    private static class PutsFunction extends BaseFunction {
        public PutsFunction() {
            super("puts", ArgumentConstraints.listOf(
                    ArgumentConstraints.typeOf(STRING, NULL),
                    ArgumentConstraints.anyValue(),
                    new MixArray(new TypeMulti(OBJECT, NULL))
            ));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var key = arguments.get(0).value();
            var value = arguments.get(1).value();
            var array = arguments.get(2).value();
            if (null == key || null == value || null == array) return array;

            var list = runtime.toList(array);
            var result = new ArrayList<T>(list.size());
            for (T v : list) {
                if (null == v) continue;
                var r = new LinkedHashMap<T, T>(2);
                for (T name : runtime.getPropertyNames(v)) {
                    r.put(name, runtime.getProperty(v, name));
                }
                r.put(key, value);
                result.add(runtime.createObject(r));
            }
            return runtime.createArray(result);
        }
    }

    public static class SliceFunction extends BaseFunction {
        public SliceFunction() {
            super("slice",
                    ArgumentConstraints.typeOf(ARRAY, NULL),
                    ArgumentConstraints.typeOf(NUMBER));
        }

        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            var value = arguments.get(0).value();
            if (null == value) {
                return null;
            }

            var list = runtime.toList(value);
            var sliceSize = runtime.toNumber(arguments.get(1).value()).intValue();

            if (list.size() < sliceSize) {
                return runtime.createArray(List.of(value));
            }

            int totalSize = list.size();
            var result = new ArrayList<T>(totalSize / sliceSize + 1);
            for (int i = 0; i < totalSize; i += sliceSize) {
                int end = Math.min(totalSize, i + sliceSize);
                var subList = list.subList(i, end);
                result.add(runtime.createArray(subList));
            }
            return runtime.createArray(result);
        }
    }

    //

    private static abstract
    class BaseArgumentConstraint implements ArgumentConstraint {
        private final int minArity;
        private final int maxArity;
        private final String expectedTypeDescription;

        public BaseArgumentConstraint(int minArity, int maxArity, String expectedTypeDescription) {
            this.minArity = minArity;
            this.maxArity = maxArity;
            this.expectedTypeDescription = expectedTypeDescription;
        }

        protected <T> Iterator<ArgumentError> checkNoRemainingArguments(
                Iterator<FunctionArgument<T>> arguments, boolean expectNoRemainingArguments) {
            if (expectNoRemainingArguments && arguments.hasNext()) {
                return singletonIterator(ArgumentError.createArityError());
            } else {
                return emptyIterator();
            }
        }

        @Override
        public int minArity() {
            return minArity;
        }

        @Override
        public int maxArity() {
            return maxArity;
        }

        @Override
        public boolean arityViolated(int n) {
            return (n < minArity || maxArity < n);
        }

        @Override
        public String expectedType() {
            return expectedTypeDescription;
        }

        protected <U> Iterator<U> singletonIterator(U obj) {
            return Collections.singleton(obj).iterator();
        }

        protected <U> Iterator<U> emptyIterator() {
            return Collections.emptyIterator();
        }
    }

    private static final String EXPRESSION_TYPE = "expression";

    private static abstract class TypeCheck extends BaseArgumentConstraint {
        public TypeCheck(String expectedType) {
            super(1, 1, expectedType);
        }

        @Override
        public <T> Iterator<ArgumentError> check(
                Adapter<T> runtime,
                Iterator<FunctionArgument<T>> arguments,
                boolean expectNoRemainingArguments) {
            if (arguments.hasNext()) {
                Iterator<ArgumentError> error = checkType(runtime, arguments.next());
                if (error.hasNext()) {
                    return error;
                } else {
                    return checkNoRemainingArguments(arguments, expectNoRemainingArguments);
                }
            } else {
                return singletonIterator(ArgumentError.createArityError());
            }
        }

        protected abstract <T> Iterator<ArgumentError> checkType(Adapter<T> runtime, FunctionArgument<T> argument);
    }

    private static class TypeMulti extends TypeCheck {
        private final Set<JmesPathType> expectedTypes;

        public TypeMulti(JmesPathType... expectedTypes) {
            super(createExpectedTypeString(expectedTypes));
            this.expectedTypes = Set.of(expectedTypes);
        }

        private static String createExpectedTypeString(JmesPathType[] expectedTypes) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < expectedTypes.length; i++) {
                buffer.append(expectedTypes[i]);
                if (i < expectedTypes.length - 2) {
                    buffer.append(", ");
                } else if (i < expectedTypes.length - 1) {
                    buffer.append(" and ");
                }
            }
            return buffer.toString();
        }

        @Override
        protected <T> Iterator<ArgumentError> checkType(Adapter<T> runtime, FunctionArgument<T> argument) {
            if (argument.isExpression()) {
                return singletonIterator(ArgumentError.createArgumentTypeError(
                        expectedType(), EXPRESSION_TYPE));
            } else {
                var actualType = runtime.typeOf(argument.value());
                if (expectedTypes.contains(actualType)) {
                    return emptyIterator();
                }
                return singletonIterator(ArgumentError.createArgumentTypeError(
                        expectedType(), JmesPathTypeNames.get(actualType)));
            }
        }
    }

    private static class MixArray extends BaseArgumentConstraint {
        private final ArgumentConstraint subConstraint;

        public MixArray(ArgumentConstraint subConstraint) {
            super(1, 1, String.format("array of %s", subConstraint.expectedType()));
            this.subConstraint = subConstraint;
        }

        @Override
        public <T> Iterator<ArgumentError> check(
                Adapter<T> runtime,
                Iterator<FunctionArgument<T>> arguments,
                boolean expectNoRemainingArguments) {
            if (arguments.hasNext()) {
                FunctionArgument<T> argument = arguments.next();
                if (argument.isExpression()) {
                    return singletonIterator(ArgumentError.createArgumentTypeError(expectedType(), EXPRESSION_TYPE));
                } else {
                    T value = argument.value();
                    JmesPathType type = runtime.typeOf(value);
                    if (type == JmesPathType.ARRAY) {
                        Iterator<ArgumentError> error = checkElements(runtime, value);
                        if (error.hasNext()) {
                            return error;
                        }
                    } else {
                        return singletonIterator(ArgumentError.createArgumentTypeError(
                                expectedType(), type.toString()));
                    }
                }
                return checkNoRemainingArguments(arguments, expectNoRemainingArguments);
            } else {
                return singletonIterator(ArgumentError.createArityError());
            }
        }

        private <T> Iterator<ArgumentError> checkElements(Adapter<T> runtime, T value) {
            List<T> elements = runtime.toList(value);
            if (!elements.isEmpty()) {
                List<FunctionArgument<T>> wrappedElements = new ArrayList<>(elements.size());
                for (T element : elements) {
                    wrappedElements.add(FunctionArgument.of(element));
                }
                Iterator<FunctionArgument<T>> wrappedElementsIterator = wrappedElements.iterator();
                while (wrappedElementsIterator.hasNext()) {
                    Iterator<ArgumentError> error = subConstraint.check(
                            runtime, wrappedElementsIterator, false);
                    if (error.hasNext()) {
                        ArgumentError e = error.next();
                        if (e instanceof ArgumentError.ArgumentTypeError) {
                            ArgumentError.ArgumentTypeError ee = (ArgumentError.ArgumentTypeError) e;
                            return singletonIterator((ArgumentError) ArgumentError.createArgumentTypeError(expectedType(), String.format("array containing %s", ee.actualType())));
                        } else {
                            return singletonIterator(e);
                        }
                    }
                }
            }
            return emptyIterator();
        }

    }

    private static final Map<JmesPathType, String> JmesPathTypeNames;

    static {
        var map = new HashMap<JmesPathType, String>();
        for (var v : values()) {
            map.put(v, v.name().toLowerCase());
        }
        JmesPathTypeNames = Map.copyOf(map);
    }
}
