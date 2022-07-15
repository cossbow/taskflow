package com.hikvision.hbfa.sf.ext.voice;

import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.config.SubmitManager;
import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.dag.CallTimeoutException;
import com.hikvision.hbfa.sf.handler.dag.data.DAGAsyncSubtask;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.handler.dag.impl.ConfigDAGNodeExecutor;
import com.hikvision.hbfa.sf.kafka.KafkaPublisher;
import com.hikvision.hbfa.sf.kafka.KafkaSubscription;
import com.hikvision.hbfa.sf.util.ExpiringCache;
import com.hikvision.hbfa.sf.util.HttpClientUtil;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.hikvision.hbfa.sf.ext.voice.VoiceTaskQueryResult.TaskStatus;

/**
 * <b>node config</b> is {@link Config}
 * <p>
 * <b>subtask input</b> is:
 * <pre><code>
 *     {
 *         "url": "audio/video stream url"
 *     }
 * </code></pre>
 * <p>
 * <b>subtask output</b> is:
 * <pre><code>
 *     {
 *         "taskID": "taskID of algorithm system",
 *         "content": "algorithm output content."
 *     }
 * </code></pre>
 */
@Slf4j
@Component
public class VoiceAnalysisComponent
        extends ConfigDAGNodeExecutor<VoiceAnalysisComponent.Config> {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1);

    //

    private final NavigableMap<String, VoiceAnalysisNode> vaNodeCache =
            new ConcurrentSkipListMap<>();
    private final NavigableMap<String, VoiceAnalysisTask> vaTaskCache =
            new ConcurrentSkipListMap<>();

    //

    private KafkaSubscription<String, VoiceTaskData> subscription;

    //

    private final ExecutorService taskExecutor;
    private final KafkaProperties properties;
    private final SubmitManager submitManager;
    private final KafkaPublisher publisher;
    private final ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache;

    private final Executor delayExecutor;

    public VoiceAnalysisComponent(ExecutorService taskExecutor,
                                  KafkaProperties properties,
                                  SubmitManager submitManager,
                                  KafkaPublisher publisher,
                                  ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache) {
        super(Config.class);
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.submitManager = submitManager;
        this.publisher = publisher;
        this.asyncSubtaskCache = asyncSubtaskCache;
        this.delayExecutor = CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS);
    }

    @PostConstruct
    void start() {
        var props = properties.buildConsumerProperties();
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1); // 一个一个来，保证顺序，反正算法慢
        subscription = new KafkaSubscription<>(
                props,
                VoiceTaskData.class,
                (s, data) -> CompletableFuture.runAsync(() -> receiveData(data), taskExecutor),
                Duration.ofSeconds(1),
                3, "voice-analysis");
        subscription.start();

        SCHEDULER.scheduleAtFixedRate(this::loopPoll, 1, 1, TimeUnit.SECONDS);
    }

    private <D> DAGResult<D> checkResponse(VoiceBaseResult<D> response) {
        if (null == response) {
            return DAGResult.error("response empty");
        }
        if (1 == response.getErrorCode()) {
            return DAGResult.success(response.data());
        }
        return DAGResult.error(response.getErrorMsg());
    }

    private Map<String, ?> makeTaskInfoParam(Collection<String> taskIDs) {
        var tasks = new ArrayList<>(taskIDs.size());
        for (String taskID : taskIDs) {
            tasks.add(Map.of("taskID", taskID));
        }
        return Map.of("taskInfo", tasks);
    }

    private <D, R extends VoiceBaseResult<D>>
    Mono<DAGResult<D>> send(HttpConfig http,
                            Map<String, String> headers,
                            Object requestBody,
                            Class<R> resultType) {

        return HttpClientUtil.<R>newBuilder()
                .method(http.method).uri(http.uri).headers(headers)
                .send(requestBody).recv(resultType).log(log).tag(http.uri).build()
                .map(this::checkResponse);
    }

    private <D, R extends VoiceBaseResult<D>>
    CompletableFuture<DAGResult<D>> sendFuture(HttpConfig http,
                                               Map<String, String> headers,
                                               Object request,
                                               Class<R> resultType) {
        return send(http, headers, request, resultType).toFuture();
    }


    private CompletableFuture<DAGResult<String>> createAnalysis(
            String nodeName, VoiceAnalysisNode node, String streamUrl) {
        var zk = submitManager.zookeeper();
        String dateStr = LocalDate.now().toString();
        var request = Map.of(
                "taskName", nodeName,
                "algorithm", Collections.singletonList(Map.of(
                        "classificationID", "27",
                        "targetType", "27",
                        "analysisType", "1",
                        "analysisSourceType", "video"
                )),
                "streamType", "localvideo",
                "audioType", "video",
                "encodeType", "mp3",
                "stream", Map.of(
                        "streamUrl", streamUrl,
                        "maxSplitCount", 1,
                        "splitTime", 1
                ),
                "time", Map.of(
                        "taskType", "temp",
                        "tempInfo", Map.of(
                                "startTime", dateStr + "T00:00:00Z",
                                "endTime", dateStr + "T23:59:59Z"
                        )
                ),
                "taskPriority", "1",
                "destination", Collections.singletonList(Map.of(
                        "destinationType", "KafkaSpeechASRNLP",
                        "destinationUrl", "{\"topic\":\"" + node.topic() + "\",\"url\":\"" + zk + "\"}"
                ))
        );
        return sendFuture(node.config.create, node.config.headers, request, VoiceTaskSubmitResult.class);
    }


    private CompletableFuture<DAGResult<List<TaskStatus>>>
    queryTasks(VoiceAnalysisNode node, Collection<String> taskIDs) {
        var http = node.config.query;
        var headers = node.config.headers;
        log.info("query tasks from {}: {}", http, taskIDs);
        if (taskIDs.size() > 512) {
            return Flux.fromIterable(taskIDs).buffer(512).flatMap(ids -> {
                return send(http, headers, makeTaskInfoParam(ids), VoiceTaskQueryResult.class);
            }).collectList().map(results -> {
                var list = new ArrayList<TaskStatus>();
                for (var result : results) {
                    if (result.isSuccess()) {
                        list.addAll(result.getData());
                    } else {
                        log.error("query tasks got error: {}", result.getError());
                    }
                }
                return DAGResult.<List<TaskStatus>>success(list);
            }).toFuture();
        }
        var request = makeTaskInfoParam(taskIDs);
        return sendFuture(http, headers, request, VoiceTaskQueryResult.class);
    }

    private List<String> dealTaskStatus(VoiceAnalysisNode node, List<TaskStatus> statuses) {
        var deleteIds = new ArrayList<String>();
        for (var status : statuses) {
            var taskId = status.getTaskId();
            if (ValueUtil.isEmpty(taskId)) {
                log.warn("taskID empty: {}", JsonUtil.lazyJson(status));
                continue;
            }
            var task = vaTaskCache.get(taskId);
            if (null == task) {
                log.warn("VoiceTask-{} not in cache", taskId);
                continue;
            }
            switch (status.getTaskStatus()) {
                case 3:
                    task.setProgress(status.getProcess());
                    break;
                case 4:
                    deleteIds.add(taskId);
                    delayExecutor.execute(() -> {
                        publisher.send(node.topic(), Map.of(
                                "voiceAnalysisFinished", Boolean.TRUE,
                                "voiceAnalysisTaskId", taskId,
                                "voiceAnalysisNodeName", node.name
                        ));
                    });
                    break;
                case 13:
                    deleteIds.add(taskId);
                    node.delId(taskId);
                    sendTaskError(task, "task failed");
                    break;
                case 14:
                    node.delId(taskId);
                    log.info("VoiceTask-{} not exists", taskId);
                    sendTaskCancel(task);
                    break;
                default:
                    log.info("VoiceTask-{} progress={}", taskId, task.getProgress());
            }
        }
        return deleteIds;
    }

    private void receiveData(VoiceTaskData data) {
        if (data.isVoiceAnalysisFinished()) {
            var taskId = data.getVoiceAnalysisTaskId();
            var task = vaTaskCache.get(taskId);
            if (null == task) {
                log.warn("VoiceTask-{} not exists or expired", taskId);
                return;
            }
            var node = vaNodeCache.get(data.getVoiceAnalysisNodeName());
            if (null == node) {
                log.warn("vaNode-{} not exists", data.getVoiceAnalysisNodeName());
                return;
            }
            node.delId(taskId);
            log.info("VoiceTask-{} finished", task.getTaskId());
            finishTask(task);
            return;
        }

        log.info("VoiceTask receive: {}", JsonUtil.lazyJson(data));
        var voiceResult = data.getVoiceResult();
        var result = checkResponse(voiceResult);
        if (!result.isSuccess()) {
            log.error("VoiceTask receive error: {}", result.getError());
            return;
        }
        if (null == voiceResult.getTargetAttrs()) {
            log.error("VoiceTask receive not contains TargetAttrs: {}", JsonUtil.lazyJson(voiceResult));
            return;
        }

        var taskId = voiceResult.getTargetAttrs().getTaskId();
        if (null == taskId) {
            log.warn("taskID empty");
            return;
        }
        var asr = voiceResult.getAsrRst();
        if (null == asr) {
            log.warn("AsrRst empty");
            return;
        }

        // TODO 插入到任务内容表
        var task = vaTaskCache.get(taskId);
        if (null == task) {
            log.error("VoiceTask-{} not exists or removed.", taskId);
            return;
        }
        if (null == asr.getStartTime()) {
            log.warn("asr empty: {}", JsonUtil.lazyJson(asr));
            return;
        }
        if (ValueUtil.isEmpty(asr.getAudioContent())) {
            log.warn("asr content empty");
            return;
        }
        log.info("VoiceTask-{} receive: {}", taskId, asr.getAudioContent());
        task.addContent(asr);
    }

    private void finishTask(VoiceAnalysisTask task) {
        var subtask = asyncSubtaskCache.del(task.getSubtaskId());
        if (null != subtask) {
            var contents = task.getContents();
            if (contents.isEmpty()) {
                log.warn("VoiceTask-{} has no content", task.getTaskId());
                subtask.complete(DAGResult.error("voice has no content"));
                return;
            }
            var output = Map.of(
                    "taskID", task.getTaskId(),
                    "contents", contents
            );
            log.info("VoiceTask-{} task content: {}",
                    task.getTaskId(), JsonUtil.lazyJson(contents));
            subtask.complete(DAGResult.success(output));
        }
    }

    private void sendTaskError(VoiceAnalysisTask task, String error) {
        var subtask = asyncSubtaskCache.del(task.getSubtaskId());
        if (null == subtask) return;
        subtask.complete(DAGResult.error(error));
    }

    private void sendTaskCancel(VoiceAnalysisTask task) {
        var subtask = asyncSubtaskCache.del(task.getSubtaskId());
        if (null == subtask) return;
        subtask.cancel(false);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<DAGResult<VoiceBaseResult<Object>>>
    deleteTask(VoiceAnalysisNode node, Collection<String> taskIDs) {
        var request = makeTaskInfoParam(taskIDs);
        return sendFuture(node.config.delete, node.config.headers, request, VoiceBaseResult.class);
    }

    private void loopPoll() {
        for (var node : vaNodeCache.values()) {
            node.query();
        }
    }

    private boolean invalid(HttpConfig http) {
        return null == http.method || ValueUtil.isEmpty(http.uri);
    }

    @Override
    protected CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, Config config, DAGNode node, Map<String, Object> input) {
        if (ValueUtil.isEmpty(config.topic)) {
            return error("topic not config");
        }
        if (invalid(config.create) || invalid(config.query) || invalid(config.delete)) {
            return error("server not config");
        }

        var vaNode = vaNodeCache.compute(node.name(), (name, van) -> {
            if (null == van) return new VoiceAnalysisNode(name, config);
            van.setConfig(config);
            return van;
        });
        var subtaskId = subtask.id();
        var url = input.get("url");
        if (null == url) return error("missing url");

        var timeout = subtask.node().timeout();
        log.info("Subtask-{} put in cache, expire in {}",
                subtask.id(), timeout);
        var future = createAnalysis(node.name(), vaNode, url.toString());

        var asyncSubtask = new DAGAsyncSubtask(subtask);
        future.whenCompleteAsync((result, ex) -> {
            if (null != ex) {
                log.error("Subtask-{} sync-call Node-{} error", subtask.id(), node.name(), ex);
                asyncSubtask.complete(DAGResult.error(ex.getMessage()));
                return;
            }
            if (!result.isSuccess()) {
                log.error("Subtask-{} create fail: {}", subtaskId, result.getError());
                asyncSubtask.complete(DAGResult.error(result.getError()));
                return;
            }

            var taskID = result.getData();
            if (ValueUtil.isEmpty(taskID)) {
                log.error("Subtask-{} create fail: no taskID return", subtaskId);
                asyncSubtask.complete(DAGResult.error("taskID empty"));
                return;
            }
            vaTaskCache.put(taskID, new VoiceAnalysisTask(subtaskId, taskID));
            vaNode.addId(taskID);
            asyncSubtaskCache.set(subtaskId, asyncSubtask, timeout).thenAcceptAsync(e -> {
                var s = e.getValue();
                log.warn("Subtask-{} Node-{} expired: {}",
                        s.subtask().id(), s.subtask().node().name(),
                        JsonUtil.lazyJson(s.subtask().input()));

                vaNode.delId(taskID);
                vaNode.delTask(Collections.singletonList(taskID));

                s.completeExceptionally(new CallTimeoutException(
                        "Subtask-" + s.subtask().id() + " timeout."));

            }, taskExecutor);
            log.info("Subtask-{} create success: taskID={}", subtaskId, taskID);
        });
        return asyncSubtask;
    }


    @Override
    public NodeType type() {
        return NodeType.VOICE_ANALYSIS;
    }

    @PreDestroy
    void stop() {
        subscription.stop();
    }

    @Data
    static class Config {
        private HttpConfig create;
        private HttpConfig query;
        private HttpConfig delete;
        private Map<String, String> headers;
        private String topic;
    }

    @Data
    static class HttpConfig {
        private HttpMethod method;
        private String uri;
    }


    private class VoiceAnalysisNode {
        private final Set<String> idSet = new ConcurrentSkipListSet<>();

        private final String name;
        private volatile Config config;
        private final CharSequence confPrint;

        private VoiceAnalysisNode(String name, Config config) {
            this.name = name;
            this.config = config;
            this.confPrint = JsonUtil.lazyJson(config);
            subscription.updateSubscribe(config.topic, "");
        }

        synchronized void setConfig(Config config) {
            var old = this.config;
            if (Objects.equals(old, config)) return;
            this.config = config;
            if (!Objects.equals(old.topic, config.topic)) {
                subscription.updateSubscribe(old.topic, "", true);
                subscription.updateSubscribe(config.topic, "");
            }
        }

        String topic() {
            return config.topic;
        }

        void addId(String id) {
            idSet.add(id);
        }

        VoiceAnalysisTask delId(String id) {
            idSet.remove(id);
            return vaTaskCache.remove(id);
        }

        void query() {
            if (idSet.isEmpty()) return;

            log.debug("poll tasks status, config-{}: {}", confPrint, idSet);
            queryTasks(this, idSet).thenAcceptAsync(result -> {
                if (!result.isSuccess()) {
                    log.error("query tasks status error: {}", result.getError());
                    return;
                }
                var statuses = result.getData();
                if (ValueUtil.isEmpty(statuses)) {
                    log.debug("query tasks status, empty.");
                    return;
                }
                var notExists = ValueUtil.subtract(idSet, statuses.stream()
                        .map(TaskStatus::getTaskId).filter(Objects::nonNull)
                        .collect(Collectors.toList()));
                for (String taskID : notExists) {
                    var task = delId(taskID);
                    if (null != task) {
                        log.error("VoiceTask-{} maybe removed", taskID);
                        sendTaskCancel(task);
                    }
                }
                log.debug("deal {} tasks", statuses.size());
                var deleteIds = dealTaskStatus(this, statuses);
                if (deleteIds.isEmpty()) return;
                log.info("delete tasks: {}", deleteIds);
                delTask(deleteIds);
            }, taskExecutor).exceptionally((e) -> {
                log.error("poll task status error, config{}", confPrint, e);
                return null;
            });
        }

        void delTask(List<String> delIds) {
            deleteTask(this, delIds).whenComplete((v, e) -> {
                if (null == e) {
                    log.info("delete tasks {} success.", delIds);
                } else {
                    log.error("delete tasks {} error", delIds, e);
                }
            });
        }

    }

}
