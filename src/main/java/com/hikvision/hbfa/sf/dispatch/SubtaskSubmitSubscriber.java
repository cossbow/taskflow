package com.hikvision.hbfa.sf.dispatch;


import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.kafka.KafkaRecvHandler;
import com.hikvision.hbfa.sf.kafka.KafkaSubscription;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import com.hikvision.hbfa.sf.util.SimpleCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 尝试添加kafka订阅，需要实现动态监听topic：
 * <ol>
 *     <li>订阅topic</li>
 *     <li>取消topic</li>
 * </ol>
 */
@Slf4j
@Component
public class SubtaskSubmitSubscriber implements KafkaRecvHandler<Long, ObjectMap> {

    private final KafkaProperties properties;

    private final DAGTaskService dagTaskService;

    private final SimpleCache<Object, DAGNode> nodeCache;
    private final ExecutorService taskExecutor;

    public SubtaskSubmitSubscriber(KafkaProperties properties,
                                   DAGTaskService dagTaskService,
                                   SimpleCache<Object, DAGNode> nodeCache,
                                   ExecutorService taskExecutor) {
        this.properties = properties;
        this.dagTaskService = dagTaskService;
        this.nodeCache = nodeCache;
        this.taskExecutor = taskExecutor;
    }

    private KafkaSubscription<Long, ObjectMap> subscription;

    @PostConstruct
    public void start() {
        subscription = new KafkaSubscription<>(
                properties.buildConsumerProperties(),
                ObjectMap.class, this,
                Duration.ofSeconds(1), 3, "Subtask");
        subscription.start();
    }

    /**
     * 更新kafka订阅topic
     *
     * @param node   要订阅的node
     * @param remove true-移除订阅，false-新增订阅
     */
    public void updateSubscribe(Node node, boolean remove) {
        if (!node.isAsync()) {
            return;
        }

        var topic = ParameterUtil.topic4Node(node.getId()).toString();
        subscription.updateSubscribe(topic, node.getId(), remove);
    }


    @Override
    public CompletableFuture<?> receive(Long nodeId, ObjectMap output) {
        var node = nodeCache.get(nodeId);
        log.debug("receive Node-{} submit: {}", node.name(), JsonUtil.lazyJson(output));

        var arg = ParameterUtil.readParam(output, node.submitId());
        if (null == arg) {
            log.error("invalid id-config({}) or output: {}", node.submitId(), output);
            return CompletableFuture.completedFuture(null);
        }

        log.debug("receive Node-{} submit Subtask-{}", node.name(), arg);
        var id = arg.toString();
        return CompletableFuture.runAsync(() -> {
            try {
                dagTaskService.submitAsync(id, output);
                log.debug("submit DAG Subtask-{} done", id);
            } catch (IllegalArgumentException e) {
                log.warn("submit DAG Subtask-{} fail", id, e);
            }
        }, taskExecutor);
    }

    @PreDestroy
    public void stop() {
        subscription.stop();
    }

}
