package com.hikvision.hbfa.sf.kafka;

import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;
import com.hikvision.hbfa.sf.handler.dag.RetryException;
import com.hikvision.hbfa.sf.util.FutureUtil;
import com.hikvision.hbfa.sf.util.RetryFuture;
import com.hikvision.hbfa.sf.util.ThrowsUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InterruptException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class KafkaSubscription<Param, T> {

    @AllArgsConstructor
    class UpdateTuple {
        private final boolean remove;
        private final String topic;
        private final Param param;
    }


    private static final VarHandle STARTED;

    static {
        MethodHandles.Lookup l = MethodHandles.lookup();
        try {
            STARTED = l.findVarHandle(KafkaSubscription.class, "started", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    //

    private final BlockingQueue<UpdateTuple> topicQueue =
            new LinkedBlockingQueue<>(100);

    private final KafkaConsumer<String, T> consumer;
    private final KafkaRecvHandler<Param, T> handler;


    private final Map<String, Param> topicParamMap = new HashMap<>();

    private final Duration timeout;
    private final int retries;
    private final String name;

    private final CompletableFuture<Void> stopFuture = new CompletableFuture<>();
    private volatile boolean started = false;
    private volatile Thread loopThread;

    public KafkaSubscription(Map<String, Object> props,
                             Class<T> type,
                             KafkaRecvHandler<Param, T> handler,
                             Duration timeout, int retries, String name) {
        this.handler = handler;
        this.retries = Math.min(0, retries);
        this.name = null != name ? name : "KafkaLoop";
        props = new HashMap<>(props);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "taskflow");
        consumer = new KafkaConsumer<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(type));
        this.timeout = Objects.requireNonNull(timeout);
    }


    public void start() {
        if (!STARTED.compareAndSet(this, false, true)) {
            log.warn("subscriber is running...");
            return;
        }

        loopThread = new Thread(this::loop, name);
        loopThread.start();

    }


    /**
     * 更新订阅
     *
     * @param topic  要订阅的topic
     * @param param  绑定参数
     * @param remove true-移除订阅，false-新增订阅
     */
    public void updateSubscribe(String topic, Param param, boolean remove) {
        Objects.requireNonNull(topic);
        try {
            log.debug("updateSubscribe '{}': remove={}", remove, topic);
            topicQueue.put(new UpdateTuple(remove, topic, param));
        } catch (InterruptedException e) {
            throw ThrowsUtil.unchecked(e);
        }
    }

    public void updateSubscribe(String topic, Param param) {
        updateSubscribe(topic, param, false);
    }


    private void checkUpdated() {
        if (topicQueue.isEmpty()) return;

        boolean updated = false;
        UpdateTuple t;
        while ((t = topicQueue.poll()) != null) {
            if (t.remove) {
                if (topicParamMap.remove(t.topic) != null) {
                    updated = true;
                }
            } else {
                // 更新node对象
                if (topicParamMap.put(t.topic, t.param) == null) {
                    updated = true;
                }
            }
        }

        if (updated) {
            if (topicParamMap.isEmpty()) {
                log.debug("unsubscribed all topics");
                consumer.unsubscribe();
            } else {
                log.debug("update subscribed topics: {}", topicParamMap.keySet());
                consumer.subscribe(topicParamMap.keySet());
            }
        }
    }

    private void pollAndConsume(List<CompletableFuture<?>> futures) throws InterruptedException {
        checkUpdated();

        if (topicParamMap.isEmpty()) {
            Thread.sleep(100);
            return;
        }

        var records = consumer.poll(timeout);
        if (records.isEmpty()) {
            return;
        }


        for (var record : records) {
            var param = topicParamMap.get(record.topic());
            CompletableFuture<?> future;
            if (retries > 0) {
                future = RetryFuture.builder(() -> {
                    return handler.receive(param, record.value());
                }).retries(retries).on(RetryException.class).build().exceptionally(e -> {
                    if (!(e instanceof CancellationException)) {
                        if (e instanceof CompletionException) {
                            e = e.getCause();
                        }
                        if (!(e instanceof CancellationException)) {
                            log.error("kafka consume", e);
                        }
                    }
                    return null;
                });
                futures.add(future);
            } else {
                try {
                    future = handler.receive(param, record.value());
                    futures.add(future);
                } catch (Throwable e) {
                    log.error("kafka consume", e);
                }
            }
        }
        FutureUtil.allOf(futures).join();
        consumer.commitSync();
    }


    private void loop() {
        var futures = new LinkedList<CompletableFuture<?>>();
        try {
            while (started) {
                try {
                    pollAndConsume(futures);
                } catch (InterruptException e) {
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable e) {
                    log.error("poll and consume error", e);
                    ThrowsUtil.sleepSilent(1000);
                } finally {
                    futures.clear();
                }
            }
        } finally {
            stopFuture.complete(null);
        }

        try {
            consumer.close();
        } catch (KafkaException e) {
            log.error("close error", e);
        }
    }

    public CompletableFuture<Void> stop() {
        started = false;
        return stopFuture;
    }

    public TaskSourceStatus status() {
        if (started) {
            return TaskSourceStatus.RUNNING;
        }
        return stopFuture.isDone() ? TaskSourceStatus.HALT : TaskSourceStatus.STOPPING;
    }

}
