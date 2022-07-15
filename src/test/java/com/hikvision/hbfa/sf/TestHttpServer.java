package com.hikvision.hbfa.sf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.util.HttpClientUtil;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.resources.LoopResources;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hikvision.hbfa.sf.TestHttpClient.*;

@Slf4j
public class TestHttpServer {

    @Data
    static class Input {
        private String submitId;
        private long value;
        private boolean makeError;
        private String topic;
        private String url;

        @Override
        public String toString() {
            return "Input{" +
                    "submitId=" + submitId +
                    ", value=" + value +
                    ", makeError=" + makeError +
                    ", topic='" + topic + '\'' +
                    '}';
        }
    }

    @Data
    static class Output {
        private final ResultCode code;
        private final String submitId;
        private final long value;
        private Instant time = Instant.now();

        @Override
        public String toString() {
            return "Output{" +
                    "code=" + code +
                    ", submitId=" + submitId +
                    ", value=" + value +
                    ", time=" + time +
                    '}';
        }
    }


    static Mono<Object> apiCall(String tag, Input input, int dv) {
        var url = input.url;
        var output = toOutput(input, dv);
        return Mono.defer(() -> {
            var subtaskId = output.submitId;
            Mono<?> req;
            if (log.isTraceEnabled()) {
                log.trace("{}: api_submit {} output: {}", tag, subtaskId, JsonUtil.lazyJson(output));
            } else {
                log.debug("{}: api_submit {} output", tag, subtaskId);
            }
            if (ValueUtil.isEmpty(url)) {
                req = submitId(subtaskId, output);
            } else {
                req = submitUrl(url, output);
            }
            return req.map(r -> {
                log.debug("{}: api_submit {} return: {}", tag, subtaskId, r);
                return Map.of();
            });
        });
    }

    static Mono<Object> sendKafka(String tag, Input input, int dv) {
        var topic = input.topic;
        if (ValueUtil.isEmpty(topic)) {
            log.error("submit topic empty: {}", input);
            throw new IllegalArgumentException("submit topic empty");
        }
        var output = toOutput(input, dv);
        return Mono.defer(() -> {
            if (log.isTraceEnabled()) {
                log.trace("{}: kafka_submit {} output: {}", tag, input.submitId, JsonUtil.lazyJson(output));
            } else {
                log.debug("{}: kafka_submit {} output", tag, input.submitId);
            }
            return Mono.fromFuture(TestKafkaClient.send(topic, output).whenCompleteAsync((v, e) -> {
                if (null == e) {
                    log.debug("{}: kafka_submit {} done.", tag, input.submitId);
                } else {
                    log.error("{}: kafka_submit {} error", tag, input.submitId, e);
                }
            }, EXECUTOR).thenApply(rm -> Map.of()));
        });
    }

    static Output toOutput(Input input, int dv) {
        return new Output(input.makeError ? ResultCode.TASK_ERROR : ResultCode.SUCCESS,
                input.submitId, input.value + dv);
    }

    abstract static class BaseHandler<T, R> implements
            BiFunction<HttpServerRequest, HttpServerResponse, NettyOutbound> {
        private final Class<T> recvType;

        BaseHandler(Class<T> recvType) {
            this.recvType = recvType;
        }

        @Override
        public NettyOutbound apply(HttpServerRequest request, HttpServerResponse response) {
            var params = request.params();
            return response.sendString(request.receive().aggregate().asString().defaultIfEmpty("null")
                    .map(s -> JsonUtil.toJson(handle(params, JsonUtil.fromJson(s, recvType)))));
        }

        abstract protected R handle(Map<String, String> params, T t);

    }

    static <T, R> BaseHandler<T, R> lambdaHandler(Class<T> recvType, Function<T, R> f) {
        return new BaseHandler<>(recvType) {
            @Override
            protected R handle(Map<String, String> params, T t) {
                return f.apply(t);
            }
        };
    }

    static <T, R> BaseHandler<T, R> lambdaHandler(Class<T> recvType, BiFunction<Map<String, String>, T, R> f) {
        return new BaseHandler<>(recvType) {
            @Override
            protected R handle(Map<String, String> params, T t) {
                return f.apply(params, t);
            }
        };
    }

    static class SimpleHandler extends BaseHandler<Input, Output> {
        final String name;
        final boolean async;
        final Submit submit;
        final int dv;

        SimpleHandler(String name, boolean async, Submit submit, int dv) {
            super(Input.class);
            this.name = name;
            this.async = async;
            this.submit = submit;
            this.dv = dv;
        }

        @Override
        protected Output handle(Map<String, String> params, Input input) {
            if (!async) {
                log.debug("{} response {}", name, input.getSubmitId());
                return toOutput(input, dv);
            }

            log.debug("{}: async_submit {} by {}", name, input.getSubmitId(), submit);
            CompletableFuture<?> future = null;
            if (submit == Submit.REST) {
                future = apiCall(name, input, dv).toFuture();
            } else if (submit == Submit.KAFKA) {
                future = sendKafka(name, input, dv).toFuture();
            }
            if (null != future) {
                future.whenComplete((v, e) -> {
                    if (null == e) {
                        log.debug("{} send complete: {}", name, JsonUtil.lazyJson(input));
                    } else {
                        log.error("{} send error", name, e);
                    }
                });
            }
            return toOutput(input, dv);
        }

    }

    static final int port = 23456;
    static final String baseUrl = "http://127.0.0.1:" + port;
    static final String TOPIC_PREFIX = "test-topic-";
    static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(4);

    static Map<String, SimpleHandler> handlers = new ConcurrentHashMap<>();

    static Map<?, ?> registerPost(
            HttpServerRoutes routes, String name, boolean async,
            NodeType type, Submit submit, int dv) {
        if (handlers.containsKey(name)) throw new IllegalStateException("name冲突: " + name);
        var path = "/" + name;
        log.info("add node[{}]: {}", name, path);
        var handler = new SimpleHandler(name, async, submit, dv);
        handlers.put(name, handler);
        routes.post(path, handler);
        var defArgs = async ? Map.of(
                "topic", "${submit.kafka.topic}",
                "url", "${['http://',submit.rest.host,submit.rest.path] | join('',@)}"
        ) : Map.of();
        var uri = baseUrl + path;
        if (NodeType.KAFKA.equals(type)) {
            var topic = TOPIC_PREFIX + name;
            subscribeCall(topic, name, submit, dv);
            return newKafkaNode(name, topic, defArgs);
        }
        return newHttpNode(HttpMethod.POST, name, uri, async, defArgs, null);
    }

    static void subscribeCall(String topic, String tag, Submit submit, int dv) {
        TestKafkaClient.subscribe(topic, Input.class, input -> {
            log.debug("{}: receive subtask-{}: {}", tag, input.submitId, input);
            // 这里简单判断一下 ***测试样例专用***
            if (submit == Submit.KAFKA) {
                return sendKafka(tag, input, dv).toFuture();
            } else {
                return apiCall(tag, input, dv).toFuture();
            }
        });
    }

    static Map<String, String> parseQuery(URI uri) {
        var pairs = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        var queries = new HashMap<String, String>(pairs.size());
        for (NameValuePair p : pairs) {
            queries.putIfAbsent(p.getName(), p.getValue());
        }
        return queries;
    }


    static final ScheduledExecutorService Scheduler = Executors.newScheduledThreadPool(4);

    static class TestVoiceAnalysisTask implements Runnable {
        private final AtomicInteger progress = new AtomicInteger(0);
        private final String id;
        private final String topic;

        private final long delay;
        private final int step;

        private volatile int status = 1;
        private volatile ScheduledFuture<?> sf;

        private final List<String> results = new ArrayList<>();

        TestVoiceAnalysisTask(String id, String topic) {
            this.id = id;
            this.topic = topic;
            this.delay = 1000;
            this.step = 10;
        }

        long randMillis() {
            return ThreadLocalRandom.current().nextLong(100, 1000);
        }

        @Override
        public void run() {
            var p = progress.addAndGet(step);
            if (p > 100) {
                status = 4;
                log.debug("task-{} finish.", id);
                return;
            }

            var time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(delay * p), ZoneOffset.UTC).toLocalTime();
            var text = ValueUtil.newUUID() + "。";
            synchronized (results) {
                results.add(text);
            }
            log.debug("task-{} progress={}", id, p);
            TestKafkaClient.send(topic, Map.of(
                    "voiceResult", Map.of(
                            "errorCode", 1,
                            "errorMsg", "ok",
                            "targetAttrs", Map.of("taskID", id),
                            "asrRst", Map.of(
                                    "startTime", "0000-00-00 " + time,
                                    "audioContent", text
                            )
                    )
            )).thenAccept(v -> {
                nextTick();
            });
        }

        void nextTick() {
            sf = Scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
        }

        void stop() {
            var f = sf;
            if (null != f) {
                f.cancel(false);
            }
        }

        String content() {
            synchronized (results) {
                return String.join("", results);
            }
        }
    }

    @Data
    static class TestVoiceAnalysisTaskInfoRequest {
        private List<TestVoiceAnalysisTaskInfo> taskInfo = List.of();

        @Data
        static class TestVoiceAnalysisTaskInfo {
            @JsonProperty("taskID")
            private String taskID;
        }
    }

    @Data
    static class TestVoiceAnalysisTaskResultRequest {
        @JsonProperty("taskID")
        private String taskID;
    }

    static void mockVoiceAnalysis(HttpServerRoutes routes) {
        var cache = new ConcurrentHashMap<String, TestVoiceAnalysisTask>();
        routes.post("/voiceAnalysis", lambdaHandler(Map.class, (o -> {
            @SuppressWarnings("unchecked")
            var destinations = (List<Map<?, ?>>) o.get("destination");
            var destinationUrl = JsonUtil.fromJson(destinations.get(0).get("destinationUrl").toString(), Map.class);
            var topic = destinationUrl.get("topic").toString();

            var id = ValueUtil.newUUID();
            var task = new TestVoiceAnalysisTask(id, topic);
            task.status = 3;
            cache.put(id, task);
            task.nextTick();
            return Map.of(
                    "errorCode", 1,
                    "errorMsg", "ok",
                    "taskID", id
            );
        })));
        routes.get("/voiceAnalysis", lambdaHandler(
                TestVoiceAnalysisTaskInfoRequest.class, (r -> {
                    var infos = r.taskInfo.stream().map(i -> {
                        var t = cache.get(i.taskID);
                        if (null == t) {
                            return Map.of(
                                    "taskID", i.taskID,
                                    "taskStatus", 14
                            );
                        }
                        return Map.of(
                                "taskID", i.taskID,
                                "taskStatus", t.status,
                                "process", t.progress
                        );
                    }).collect(Collectors.toList());
                    return Map.of(
                            "errorCode", 1,
                            "errorMsg", "ok",
                            "status", infos
                    );
                })
        ));
        routes.delete("/voiceAnalysis", lambdaHandler(
                TestVoiceAnalysisTaskInfoRequest.class, r -> {
                    for (var i : r.taskInfo) {
                        var t = cache.get(i.taskID);
                        if (null != t) {
                            t.stop();
                        }
                    }
                    return Map.of("errorCode", 1, "errorMsg", "ok");
                }
        ));
        routes.get("/voiceAnalysisResult/{taskID}", lambdaHandler(Object.class, (p, r) -> {
            var taskID = p.get("taskID");
            var t = cache.remove(taskID);
            return Map.of("content", t.content());
        }));
        routes.get("/voiceAnalysisClear", lambdaHandler(Object.class, (p, r) -> {
            log.info("voiceAnalysisClear");
            cache.clear();
            return Map.of();
        }));
        //
        TestHttpClient.registerNodes(
                TestHttpClient.newNode("testVoiceAnalysis",
                        NodeType.VOICE_ANALYSIS, true,
                        Map.of(
                                "create", Map.of("method", "POST", "uri", baseUrl + "/voiceAnalysis"),
                                "query", Map.of("method", "GET", "uri", baseUrl + "/voiceAnalysis"),
                                "delete", Map.of("method", "DELETE", "uri", baseUrl + "/voiceAnalysis"),
                                "topic", "test.workflow.topic.voice-analysis"
                        ),
                        null, null, 60_000)
        ).block();
        log.info("add node[testVoiceAnalysis]: {}", baseUrl);
    }

    /**
     * 创建三个节点：
     * <ol>
     *     <li>parse</li>
     *     <li>nlp</li>
     *     <li>imgSearch</li>
     * </ol>
     */
    public static void main(String[] args) throws InterruptedException {
        final int dv = 1;
        // HTTP Restful server
        var loop = LoopResources.create("HttpClient", 1,
                1, true);
        HttpServer.create().runOn(loop).port(port).route(routes -> {
            registerNodes(
                    registerPost(routes, "parse", false, NodeType.REST, null, dv),
                    registerPost(routes, "human", true, NodeType.REST, Submit.KAFKA, dv),
                    registerPost(routes, "face", true, NodeType.REST, Submit.KAFKA, dv),
                    registerPost(routes, "vehicle", true, NodeType.REST, Submit.KAFKA, dv),
                    registerPost(routes, "nlp", true, NodeType.REST, Submit.KAFKA, dv),
                    registerPost(routes, "search", false, NodeType.REST, null, dv),
                    registerPost(routes, "ocr", true, NodeType.KAFKA, Submit.KAFKA, dv),
                    registerPost(routes, "plus", false, NodeType.REST, null, 10),
                    registerPost(routes, "minus", false, NodeType.REST, null, -10)
            ).block();
            routes.get("/query", (request, response) -> {
                var uri = URI.create(request.uri());
                var queries = parseQuery(uri);
                var value = Long.parseLong(queries.get("value"));
                var submitId = queries.get("id");
                var output = new Output(ResultCode.SUCCESS, submitId, value);
                return response.send(HttpClientUtil.createJsonBuf(output));
            });
            registerNodes(
                    newHttpNode(
                            HttpMethod.GET, "query", baseUrl + "/query", false,
                            Map.of("id", "${id}", "value", "${task.input.value}"),
                            Map.of("submitId", "${id}", "value", "${value}")))
                    .subscribe();
            routes.post("/subtask/notify", (request, response) -> {
                var re = request.receive().aggregate().asString().map(s -> {
                    log.debug("subtask complete: {}", s);
                    return ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, s);
                });
                return response.send(re);
            });
            routes.post("/task/notify", (request, response) -> {
                var re = request.receive().aggregate().asString().map(s -> {
                    log.debug("task complete: {}", s);
                    return ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, s);
                });
                return response.send(re);
            });
            // mock音频分析接口
            mockVoiceAnalysis(routes);
        }).bindNow();
        log.info("baseUrl: " + baseUrl);

        synchronized (sync) {
            sync.wait();
        }
    }

    static final Object sync = new Object();

}
