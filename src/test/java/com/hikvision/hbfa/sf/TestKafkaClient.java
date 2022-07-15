package com.hikvision.hbfa.sf;

import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.kafka.KafkaPublisher;
import com.hikvision.hbfa.sf.kafka.KafkaSubscription;
import com.hikvision.hbfa.sf.util.FutureUtil;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
public class TestKafkaClient {
    static final ScheduledExecutorService executor =
            new ScheduledThreadPoolExecutor(100);
    static final KafkaPublisher Publisher;

    static {
        var properties = new KafkaProperties();
        Publisher = new KafkaPublisher(properties);
    }

    static final Duration timeout = Duration.ofSeconds(2);

    public static CompletableFuture<RecordMetadata>
    send(String topic, Object value) {
        return Publisher.send(topic, value);
    }


    static <T> void subscribe(String topic, Class<T> type,
                              Function<T, CompletableFuture<?>> handler) {
        var props = new KafkaProperties();
        props.getConsumer().setGroupId("test");
        var sub = new KafkaSubscription<>(props.buildConsumerProperties(), type,
                (o, t) -> handler.apply(t).thenAccept(FutureUtil.doNothing()),
                Duration.ofSeconds(1), 9, "test");
        sub.updateSubscribe(topic, Object.class);
        sub.start();
    }

    static void startWorkflow(String name, boolean makeError) {
        var value = ThreadLocalRandom.current().nextInt(1, (Integer.MAX_VALUE - 100) >> 1);
        var prices = TestHttpClient.randInts();
        var data = Map.of(
                "name", name,
                "input", Map.of("value", value, "prices", prices, "makeError", makeError),
                "timeout", 600_000,
                "verbose", true
        );
        send(ParameterUtil.TOPIC_TASK_START, data).join();
    }

    static void startRandValue(String topic) {
        var value = ThreadLocalRandom.current().nextInt(1, (Integer.MAX_VALUE - 100) >> 1);
        var prices = TestHttpClient.randInts();
        log.debug("start rand: {}, {}", value, prices);
        send(topic, Map.of("value", value, "prices", prices)).whenComplete((x, e) -> {
            if (null == e) {
                log.debug("push {} done.", topic);
            } else {
                log.debug("push {} error", topic, e);
            }
        });
    }

    @Test
    public void testStart() {
        startWorkflow(TestHttpClient.workflowName, false);
    }

    public static void main(String[] args) throws InterruptedException {
        var scheduler = Executors.newScheduledThreadPool(4);
        Runnable r = () -> startRandValue("test-search-1");
        scheduler.scheduleWithFixedDelay(r, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(r, 111, 777, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(r, 333, 333, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(r, 222, 111, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(r, 666, 11, TimeUnit.MILLISECONDS);

        synchronized (timeout) {
            timeout.wait();
        }
    }

}
