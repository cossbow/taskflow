package com.hikvision.hbfa.sf;

import com.fasterxml.jackson.databind.JavaType;
import com.hikvision.hbfa.sf.dto.WorkflowDto;
import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.ex.HttpStatusException;
import com.hikvision.hbfa.sf.util.HttpClientUtil;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.hikvision.hbfa.sf.util.MapUtil.nonnull;

@Slf4j
public class TestHttpClient {

    static final String flowServer;

    static {
        var port = System.getProperty("server.port", "9457");
        flowServer = "http://127.0.0.1:" + port + "/v1";
    }


    @Data
    static class Response<T> {
        private int code;
        private T data;
        private String msg;

        @Override
        public String toString() {
            return "Response{" +
                    "code=" + code +
                    ", data=" + data +
                    ", msg='" + msg + '\'' +
                    '}';
        }
    }

    static final Map<JavaType, JavaType> responseTypeMap = new ConcurrentHashMap<>();


    static JavaType typeTo(Class<?> t) {
        return JsonUtil.typeFactory().constructType(t);
    }

    static JavaType respType(JavaType javaType) {
        return responseTypeMap.computeIfAbsent(javaType, t ->
                JsonUtil.typeFactory().constructParametricType(Response.class, t));
    }


    static <T> Mono<T> restful(HttpMethod method, String uri, Object data, JavaType type) {
        return HttpClientUtil.<T>newBuilder().method(method).uri(uri).send(data).recv(type)
                .log(log).tag(uri).build();
    }

    static <T> Mono<T> restful(HttpMethod method, String uri, Object data, Class<T> type) {
        return restful(method, uri, data, typeTo(type));
    }

    static <T> Mono<T> testServer(HttpMethod method, String uri, Object data, Class<T> type) {
        return restful(method, TestHttpServer.baseUrl + uri, data, type);
    }

    static <T> Mono<T> restFlow(HttpMethod method, String uri, Object data, JavaType type) {
        var url = flowServer + uri;
        return TestHttpClient.<Response<T>>restful(method, url, data, respType(type)).map(r -> {
            if (r.code > 0) {
                throw new IllegalStateException("code " + r.code);
            }
            return r.data;
        });
    }

    static <T> Mono<T> restSend(HttpMethod method, String uri, Object data, Class<T> type) {
        return restFlow(method, uri, data, typeTo(type));
    }

    static Mono<Object> restSend(HttpMethod method, String uri, Object data) {
        return restFlow(method, uri, data, typeTo(Object.class));
    }

    static Map<String, Object> newSubmitConfig(Submit submit) {
        var submitConfig = new HashMap<String, Object>();
        if (null == submit) {
            return submitConfig;
        }
        submitConfig.put("parameters", Map.of(
                "id", "${submitId}"
        ));
        return submitConfig;
    }

    static Map<?, ?> notifierConfig(String path) {
        return Map.of(
                "method", "POST",
                "uri", TestHttpServer.baseUrl + path,
                "headers", headers()
        );
    }

    static Mono<Object> registerNodes(List<Map<?, ?>> data) {
        return restSend(HttpMethod.POST, "/meta/nodes", data);
    }

    static Mono<Object> registerNodes(Map<?, ?>... data) {
        return registerNodes(List.of(data));
    }

    static Map<?, ?> newNode(String name, NodeType type, boolean async,
                             Map<?, ?> config, Map<?, ?> defaultArguments,
                             Consumer<HashMap<String, Object>> more, long timeout) {
        var data = new HashMap<>(Map.of(
                "name", name,
                "type", type,
                "config", nonnull(config),
                "defaultArguments", nonnull(defaultArguments),
                "async", async,
                "submitConfig", Map.of(
                        "parameters", Map.of("id", "${submitId}"),
                        "timeout", timeout
                ),
//                "notifier", "REST",
//                "notifierConfig", notifierConfig("/subtask/notify"),
                "remark", name
        ));
        if (null != more) more.accept(data);
        return data;
    }

    static Map<?, ?> newNode(String name, NodeType type, boolean async,
                             Map<?, ?> config, Map<?, ?> defaultArguments,
                             Consumer<HashMap<String, Object>> more) {
        return newNode(name, type, async, config, defaultArguments, more, 360_000);
    }

    static Map<?, ?> newKafkaNode(String name, String topic, Map<?, ?> defArgs) {
        return newNode(name, NodeType.KAFKA, true, Map.of(
                "topic", topic
        ), defArgs, null);
    }

    static Map<?, ?> newHttpNode(HttpMethod method,
                                 String name, String uri, boolean async,
                                 Map<?, ?> defArgs, Map<?, ?> queries) {
        return newNode(name, NodeType.REST, async, Map.of(
                "method", method.name(),
                "uri", uri,
                "headers", headers(),
                "queries", nonnull(queries)
        ), defArgs, null);
    }

    static Map<?, ?> newScriptNode(String name, ScriptType type, String text) {
        return newNode(name, NodeType.SCRIPT, false, Map.of(
                "type", type,
                "text", text
        ), null, null);
    }

    static Map<?, ?> newSubflowNode(String name, String workflowName) {
        return newNode(name, NodeType.SUBFLOW, true, Map.of(
                "workflow", workflowName
        ), null, null);
    }

    static Map<String, String> headers() {
        return Map.of("author", "jjj@cossbow.com");
    }


    /**
     * @see TestHttpClient#workflowOutput
     */
    @Data
    static class TaskOutput {
        Object taskId;
        Object subtaskId;
        long taskInputValue;
        long parseValue;
        long searchValue;
        long sumValue;
        long humansBatch;
        long analyseTextDoc;
        long decisionValue;
        long queryValue;

        @Override
        public String toString() {
            return "TaskOutput{" +
                    "taskId=" + taskId +
                    ", subtaskId=" + subtaskId +
                    ", taskInputValue=" + taskInputValue +
                    ", parseValue=" + parseValue +
                    ", searchValue=" + searchValue +
                    ", sumValue=" + sumValue +
                    ", analyseTextDoc=" + analyseTextDoc +
                    ", humansBatch=" + humansBatch +
                    ", humansBatch=" + decisionValue +
                    ", queryValue=" + queryValue +
                    '}';
        }
    }

    /**
     * @see TaskOutput
     */
    static Map<String, Object> workflowOutput() {
        return Map.of(
                "taskId", "${task.id}", // 获取任务id
                "subtaskId", "${id}", // 获取子任务id
                "taskInputValue", "${task.input.value}",
                "parseValue", "${parse.output.value}",
                "analyseTextDoc", "${AnalyseTextDoc.output.value}",
                "searchValue", "${search.output.value}",
                "sumValue", "${sum.output.value}",
                "humansBatch", "${humansBatch.output.value}",
                "decisionValue", "${decision.output.value}",
                "queryValue", "${query.output.value}"
        );
    }

    static Map<?, ?> newIO(String name, String valueParam) {
        return newIO(name, valueParam, false);
    }

    static Map<?, ?> newIO(String name, String valueParam, boolean ignoreError) {
        return newIO(name, Map.of(
                "value", valueParam,
                "makeError", "${task.input.makeError}",
                "submitId", "${id}"
        ), ignoreError);
    }

    static Map<?, ?> newIO(String name) {
        return newIO(name, (Map<?, ?>) null, false);
    }

    static Map<?, ?> newIO(String name, Map<?, ?> inputParameters, boolean ignoreError) {
        return null == inputParameters ? Map.of(
                "nodeName", name,
                "ignoreError", ignoreError
        ) : Map.of(
                "nodeName", name,
                "inputParameters", inputParameters,
                "ignoreError", ignoreError
        );
    }

    static Map<?, ?> newEdge(String fromName, String toName) {
        return Map.of(
                "fromNode", fromName,
                "toNode", toName
        );
    }

    static Mono<Long> newWorkflow(
            String name, Map<String, Object> output,
            List<?> nodes, List<?> edges,
            CallType notifier) {
        var data = Map.of(
                "name", name,
                "outputParameters", output,
                "notifierConfig", notifierConfig("/task/notify"),
                "remark", "search documents",
                "nodes", nodes,
                "edges", edges
        );
        data = new HashMap<>(data);
        if (null != notifier) data.put("notifier", notifier);
        return TestHttpClient.<WorkflowDto>restFlow(
                HttpMethod.POST, "/meta/workflow", data, typeTo(WorkflowDto.class))
                .map(WorkflowDto::getId);
    }

    /**
     * 创建工作流：
     */
    static Mono<Long> createWorkflow(String name) {
        // 创建工作流
        var ios = List.of(
                newIO("parse", "${task.input.value}"),
                newIO("human", "${parse.output.value}", true),
                newIO("face", "${parse.output.value}", true),
                newIO("vehicle", "${parse.output.value}", true),
                newIO("search", "${sum([human.output.value,face.output.value,vehicle.output.value])}"),
                newIO("sum", "${[search.output.value,AnalyseTextDoc.output.value]}"),
                newIO("humansBatch",
                        "${task.input.prices[].{value:@}}"),
                newIO("AnalyseTextDoc", "${face.output.value}"),
                newIO("decision", "${task.input.value}"),
                newIO("query", "${task.input.value}")
        );
        var edges = List.of(
                newEdge("parse", "human"),
                newEdge("parse", "face"),
                newEdge("parse", "vehicle"),
                newEdge("parse", "decision"),
                newEdge("human", "search"),
                newEdge("face", "search"),
                newEdge("vehicle", "search"),
                newEdge("human", "humansBatch"),
                newEdge("face", "humansBatch"),
                newEdge("face", "AnalyseTextDoc"),
                newEdge("search", "sum"),
                newEdge("AnalyseTextDoc", "sum")
        );
        // 创建一个workflow
        return newWorkflow(name, workflowOutput(),
                ios, edges, null
        );
    }

    /**
     * 创建一个子流程
     */
    static Mono<Long> createAnalyseTextDoc(String name) {
        var ios = List.of(
                newIO("ocr", "${task.input.value}"),
                newIO("nlp", "${ocr.output.value}")
        );
        var edges = List.of(
                newEdge("ocr", "nlp")
        );
        // 创建一个workflow
        return newWorkflow(name, Map.of(
                "value", "${nlp.output.value}"
                ),
                ios, edges, null
        );
    }

    static Mono<Long> createSingle(String node) {
        var ios = List.of(newIO(node, "${task.input.value}"));
        // 创建一个workflow
        return newWorkflow(node, Map.of(
                "value", "${" + node + ".output.value}"
                ),
                ios, List.of(), null
        );
    }

    static Mono<Long> createVoiceAnalysis(String node) {
        var contentJoin = node + "ContentJoin";
        var newContentJoin = registerNodes(newNode(node + "ContentJoin", NodeType.SCRIPT, false, Map.of(
                "type", "JMESPath",
                "text", "def(contents,`[]`) | sort_by(@, &startTime) | [].audioContent | {content:join('',@)}"
        ), null, null));
        var ios = List.of(newIO(node,
                Map.of("url", "${task.input.url}"),
                false), newIO(contentJoin,
                Map.of("contents", "${" + node + ".output.contents}"), false));
        var edges = List.of(newEdge(node, contentJoin));
        // 创建一个workflow
        return newContentJoin.then(newWorkflow(node, Map.of(
                "fileId", "${task.input.fileId}",
                "content", "${" + contentJoin + ".output.content}",
                "taskID", "${" + node + ".output.taskID}"
        ), ios, edges, CallType.REST));
    }


    @Data
    static class Input {
        private long value;
    }

    @Data
    static class TaskDto {
        private String id;
        private long workflowId;
        private String tag;
        private Input input;
        private boolean success;
        private TaskOutput output;
        private String errMsg;
        private Instant createdAt;
        private Instant updatedAt;
    }

    static Mono<TaskDto> callWorkflow(String name, int value, int[] prices, boolean makeError) {
        return callWorkflow(name,
                Map.of("value", value, "prices", prices, "makeError", makeError),
                TaskDto.class, 600_000);
    }

    static <R> Mono<R> callWorkflow(String name, Object input, Class<R> rt, long timeout) {
        var data = Map.of(
                "name", name,
                "input", input,
                "timeout", timeout,
                "verbose", true
        );
        return restSend(HttpMethod.POST, "/task/call", data, rt);
    }


    static Mono<Object> submitId(Object subtaskId, Object output) {
        return restSend(HttpMethod.POST, "/task/submit/" + subtaskId, output);
    }

    static Mono<Object> submitUrl(String submitUrl, Object output) {
        return restSend(HttpMethod.POST, submitUrl, output);
    }

    static int[] randInts() {
        var rand = ThreadLocalRandom.current();
        return rand.ints(rand.nextInt(2, 10), 100, 200).toArray();
    }

    static final String workflowName = "SearchImg";

    static private Map<String, Object> newBranch(String workflow, ScriptType type, String text) {
        return Map.of("workflow", workflow, "rule", Map.of("type", type, "text", text));
    }

    private final AtomicBoolean testCreateFlowsOnce = new AtomicBoolean(false);

    void testCreateFlows() {
        if (!testCreateFlowsOnce.compareAndSet(false, true)) return;

        registerNodes(
                newNode("sum", NodeType.SCRIPT, false, Map.of(
                        "type", ScriptType.JMESPath,
                        "text", "{value:sum(value)}"
                ), null, null),
                newNode("humansBatch", NodeType.BATCH, false, Map.of(
                        "nodeName", "human",
                        "dataKey", "value",
                        "forkParameters", Map.of(
                                "value", "${input.value}",
                                "submitId", "${id}"
                        ),
                        "joinParameters", Map.of(
                                "value", "${value[].value | sum(@)}"
                        )
                ), null, null),
                newSubflowNode("AnalyseTextDoc", "AnalyseTextDoc"),
                newNode("decision", NodeType.DECISION, false, Map.of(
                        "branches", List.of(
                                newBranch("plus", ScriptType.QLExpress, "value % 2 == 0"),
                                newBranch("minus", ScriptType.QLExpress, "value % 2 != 0")
                        )
                ), null, null)
        ).block();

        createAnalyseTextDoc("AnalyseTextDoc").block();
        createSingle("plus").block();
        createSingle("minus").block();

        // 创建一个workflow
        createWorkflow(workflowName).block();
    }


    @Data
    static class VoiceAnalysisOutput {
        private String fileId;
        private String taskID;
        private String content;
    }

    @Data
    static class VoiceAnalysisTaskDto {
        private String id;
        private long workflowId;
        private String tag;
        private boolean success;
        private VoiceAnalysisOutput output;
        private String errMsg;
    }

    @Test
    public void testVoiceAnalysis() {
        var name = "testVoiceAnalysis";
        createVoiceAnalysis(name).block();

        var dto = callWorkflow(name, Map.of("fileId",
                ValueUtil.newUUID(), "url", "http://url"),
                VoiceAnalysisTaskDto.class, 360_000).block();
        assert dto != null;
        var output = dto.output;
        var actual = output.getContent();
        var result = testServer(HttpMethod.GET, "/voiceAnalysisResult/" + output.taskID,
                null, VoiceAnalysisOutput.class).block();
        assert result != null;
        var expect = result.getContent();
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testVoiceAnalysisError() {
        var name = "testVoiceAnalysis";
        createVoiceAnalysis(name).block();

        var request = callWorkflow(name, Map.of("fileId",
                ValueUtil.newUUID(), "url", "http://url"),
                VoiceAnalysisTaskDto.class, 360_000);
        Mono.delay(Duration.ofSeconds(2)).flatMap(i -> {
            return testServer(HttpMethod.GET, "/voiceAnalysisClear",
                    null, Object.class);
        }).subscribe(o -> {
            log.info("clear success.");
        });

        try {
            request.block();
        } catch (HttpStatusException e) {
            Assert.assertEquals(409, e.getStatus());
        }

    }


    //

    private void validOutput(long value, int[] prices, TaskDto dto) {
        if (dto.success) {
            log.debug("Task-{} finish: {}", dto.getId(), JsonUtil.lazyJson(dto.getOutput()));
        } else {
            log.info("Task-{} finish: {}", dto.getId(), JsonUtil.lazyJson(dto.getOutput()));
            Assert.fail("Task-" + dto.getId() + " error: " + dto.getOutput());
        }

        Assert.assertEquals(value, dto.input.value);

        var output = dto.output;
        Assert.assertEquals(value, output.taskInputValue);
        Assert.assertEquals(value, output.queryValue);
        Assert.assertEquals(value + 1, output.parseValue);
        Assert.assertEquals((value + 2) * 3L + 1, output.searchValue);
        Assert.assertEquals(value + 4, output.analyseTextDoc);
        Assert.assertEquals((output.searchValue + output.analyseTextDoc), output.sumValue);
        var humansBatch = Arrays.stream(prices).map(i -> i + 1).sum();
        Assert.assertEquals(humansBatch, output.humansBatch);
        Assert.assertEquals(value + (value % 2 == 0 ? 10 : -10), output.decisionValue);
    }

    @Test
    public void testCallFlow() {
        testCreateFlows();

        var value = ThreadLocalRandom.current().nextInt(1, (Integer.MAX_VALUE - 100) >> 1);
        var prices = randInts();

        var result = callWorkflow(workflowName, value, prices, false).block();
        Assert.assertNotNull(result);
        validOutput(value, prices, result);
    }

    private void callFlowConcurrent(int concurrent) {
        testCreateFlows();

        var rand = ThreadLocalRandom.current();
        var monoList = new ArrayList<Mono<Boolean>>(concurrent);
        for (int k = 0; k < concurrent; k++) {
            var value = rand.nextInt(1, Short.MAX_VALUE << 1);
            var prices = randInts();
            var mono = callWorkflow(workflowName, value, prices, false).map(result -> {
                validOutput(value, prices, result);
                return true;
            });
            monoList.add(mono);
        }

        var t0 = System.currentTimeMillis();
        Flux.merge(monoList).collectList().block();
        var t1 = System.currentTimeMillis();
        log.info("finished when Concurrent={}, using {}s", concurrent, (t1 - t0) * 0.001);
    }


    public static void main(String[] args) {
        int concurrent = 100;
        int step = 0;
        int max = 10000;
        if (args.length > 0) {
            concurrent = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            step = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            max = Integer.parseInt(args[2]);
        }

        var test = new TestHttpClient();
        if (step > 0) {
            for (int n = step; n <= max; n += step) {
                try {
                    test.callFlowConcurrent(n);
                } catch (Exception e) {
                    Assert.fail("Failed when concurrent=" + n);
                    throw e;
                }
            }
        } else {
            test.callFlowConcurrent(concurrent);
        }
    }
}
