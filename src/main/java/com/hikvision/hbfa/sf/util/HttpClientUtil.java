package com.hikvision.hbfa.sf.util;

import com.fasterxml.jackson.databind.JavaType;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hikvision.hbfa.sf.ex.HttpStatusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.ReactorNetty;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static reactor.netty.NettyPipeline.OnChannelReadIdle;
import static reactor.netty.NettyPipeline.OnChannelWriteIdle;

@Slf4j
final
public class HttpClientUtil {
    private HttpClientUtil() {
    }

    //

    public static final BiConsumer<HttpClientResponse, Connection> HTTP_OK_CHECKER
            = httpStatusChecker(sc -> 200 <= sc.code() && sc.code() < 300);

    //

    static final HttpClient DefaultClient;
    static final HttpClient StatusCheckedClient;
    static final HttpClient RestfulClient;

    static {
        var loop = LoopResources.create("HttpClient", 1,
                ValueUtil.getThreadCount(ReactorNetty.IO_WORKER_COUNT), true);
        DefaultClient = HttpClient.create().runOn(loop).secure(spec -> {
            var builder = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE);
            try {
                spec.sslContext(builder.build());
            } catch (SSLException e) {
                throw new UncheckedIOException(e);
            }
        }).compress(true).responseTimeout(Duration.ofHours(1));
        StatusCheckedClient = DefaultClient.doOnResponse(HTTP_OK_CHECKER);
        RestfulClient = StatusCheckedClient.headers(headers -> {
            headers.set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
            headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        });
    }

    public static HttpClient client() {
        return DefaultClient;
    }

    public static HttpClient checkingClient() {
        return StatusCheckedClient;
    }

    public static HttpClient restfulClient() {
        return RestfulClient;
    }

    public static HttpClient checkingClient(Predicate<HttpResponseStatus> okFilter) {
        return client().doOnResponse(httpStatusChecker(okFilter));
    }


    public static Function<TcpClient, TcpClient>
    timeoutHandler(long writeTimeout, long readTimeout, TimeUnit timeUnit) {
        return tcpClient -> tcpClient.doOnConnected(conn -> {
            var write = new WriteTimeoutHandler(writeTimeout, timeUnit);
            var read = new ReadTimeoutHandler(readTimeout, timeUnit);
            conn.removeHandler(OnChannelWriteIdle)
                    .addHandlerFirst(OnChannelWriteIdle, write);
            conn.removeHandler(OnChannelReadIdle)
                    .addHandlerFirst(OnChannelReadIdle, read);
        });
    }

    public static Function<TcpClient, TcpClient>
    timeoutMillisHandler(long writeTimeout, long readTimeout) {
        return timeoutHandler(writeTimeout, readTimeout, TimeUnit.MILLISECONDS);
    }

    public static Function<TcpClient, TcpClient>
    timeoutSecondsHandler(long writeTimeout, long readTimeout) {
        return timeoutHandler(writeTimeout, readTimeout, TimeUnit.SECONDS);
    }

    public static BiConsumer<HttpClientResponse, Connection>
    httpStatusChecker(Predicate<HttpResponseStatus> okFilter) {
        return (response, connection) -> {
            var status = response.status();
            if (!okFilter.test(status)) {
                throw new HttpStatusException(status.code());
            }
        };
    }

    public static Consumer<? super HttpHeaders> headerBuilder(Map<String, String> headerMap) {
        return (Consumer<HttpHeaders>) headers -> {
            for (var entry : headerMap.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }
        };
    }

    public static Mono<ByteBuf> createByteBuf(FailableConsumer<ByteBufOutputStream, IOException> writer) {
        Objects.requireNonNull(writer);

        return Mono.create(sink -> {
            ByteBuf buf;
            try {
                buf = ByteBufAllocator.DEFAULT.buffer();
            } catch (Throwable e) {
                sink.error(e);
                return;
            }
            try (var out = new ByteBufOutputStream(buf)) {
                writer.accept(out);
                sink.success(buf);
                sink.onDispose(() -> {
                    if (buf.refCnt() > 0) {
                        ReferenceCountUtil.safeRelease(buf);
                    }
                });
            } catch (Throwable e) {
                ReferenceCountUtil.safeRelease(buf);
                sink.error(e);
            }
        });
    }

    public static Mono<ByteBuf> createJsonBuf(Object o, Mono<ByteBuf> defRet) {
        if (null == o) return defRet;
        return createByteBuf(out -> JsonUtil.writeJson(out, o));
    }

    public static Mono<ByteBuf> createJsonBuf(Object o) {
        return createJsonBuf(o, Mono.empty());
    }

    private static final Map<org.springframework.http.HttpMethod, HttpMethod> MethodMap;
    private static final LoadingCache<String, HttpMethod> MethodCache;

    static {
        MethodMap = new EnumMap<>(org.springframework.http.HttpMethod.class);
        for (var m : org.springframework.http.HttpMethod.values()) {
            MethodMap.put(m, HttpMethod.valueOf(m.name()));
        }
        MethodCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .softValues().build(HttpMethod::valueOf);
    }


    //
    //
    //

    public static <R> HttpBuilder<R> newBuilder() {
        return new HttpBuilder<>();
    }

    /**
     * restful request wrapper
     */
    public static class HttpBuilder<R> {
        private HttpClient client = restfulClient();
        private HttpMethod method = HttpMethod.GET;
        private Mono<String> uri;
        private Map<String, String> headers = null;
        private Object requestBody = null;
        private JavaType responseType = null;

        private boolean logTrace;
        private Logger log = HttpClientUtil.log;
        private String tag;

        // 请求序列（随机数），用于日志检查时便于配对请求和响应
        private final String seq =
                Integer.toHexString(
                        ThreadLocalRandom.current().nextInt(100000, 99999999));

        //


        public HttpBuilder<R> client(HttpClient c) {
            this.client = Objects.requireNonNull(c, "client");
            return this;
        }

        public HttpBuilder<R> method(HttpMethod method) {
            this.method = Objects.requireNonNull(method, "method");
            return this;
        }

        public HttpBuilder<R> method(org.springframework.http.HttpMethod method) {
            return method(MethodMap.get(method));
        }

        public HttpBuilder<R> method(String method) {
            return method(MethodCache.get(method));
        }

        public HttpBuilder<R> get() {
            return method(HttpMethod.GET);
        }

        public HttpBuilder<R> post() {
            return method(HttpMethod.POST);
        }

        public HttpBuilder<R> put() {
            return method(HttpMethod.PUT);
        }

        public HttpBuilder<R> delete() {
            return method(HttpMethod.DELETE);
        }


        //

        public HttpBuilder<R> uri(Mono<String> uri) {
            this.uri = Objects.requireNonNull(uri, "uri");
            return this;
        }

        public HttpBuilder<R> uri(String uri) {
            return uri(Mono.just(Objects.requireNonNull(uri, "uri")));
        }

        public HttpBuilder<R> uri(URI uri) {
            return uri(Mono.fromSupplier(Objects.requireNonNull(uri, "uri")::toString));
        }

        //

        public HttpBuilder<R> headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        //

        /**
         * default null, will not send request body
         *
         * @param body send data object
         */
        public HttpBuilder<R> send(Object body) {
            this.requestBody = body;
            return this;
        }

        /**
         * default null, will discard response body
         *
         * @param responseType json type for parse response body
         */
        public HttpBuilder<R> recv(JavaType responseType) {
            this.responseType = responseType;
            return this;
        }

        /**
         * @see #recv(JavaType)
         */
        public HttpBuilder<R> recv(Class<R> responseClass) {
            return recv(JsonUtil.typeOf(responseClass));
        }

        //

        /**
         * default use log of HttpClientUtil
         *
         * @param log log that print by
         */
        public HttpBuilder<R> log(Logger log) {
            this.log = Objects.requireNonNull(log, "log");
            return this;
        }

        /**
         * set log tag which used by differentiate request & response
         */
        public HttpBuilder<R> tag(String t) {
            this.tag = t;
            return this;
        }

        /**
         * set log level to TRACE, default DEBUG
         */
        public HttpBuilder<R> trace(boolean t) {
            logTrace = t;
            return this;
        }

        //

        private Mono<ByteBuf> encodeRequest() {
            if (logTrace) {
                log.trace("[{}]request-{}: {}", seq, tag, JsonUtil.lazyJson(requestBody));
            } else {
                log.debug("[{}]request-{}: {}", seq, tag, JsonUtil.lazyJson(requestBody));
            }
            return createJsonBuf(requestBody);
        }

        private HttpClient.RequestSender prepare() {
            return client.request(method).uri(uri);
        }

        private HttpClient.ResponseReceiver<?> send() {
            if (ValueUtil.isEmpty(headers)) {
                return prepare().send(encodeRequest());
            } else {
                return prepare().send((request, outbound) -> {
                    headers.forEach(request::addHeader);
                    return outbound.send(encodeRequest());
                });
            }
        }

        public Mono<R> build() {
            if (null == responseType) {
                return send().response().then(Mono.empty());
            }

            var dataMono = send().responseContent().aggregate();
            if (logTrace) {
                if (log.isTraceEnabled()) {
                    return dataMono.asString().map(s -> {
                        log.trace("[{}]response: {}", seq, s);
                        return JsonUtil.fromJson(s, responseType);
                    });
                }
            } else {
                if (log.isDebugEnabled()) {
                    return dataMono.asString().map(s -> {
                        log.debug("[{}]response: {}", seq, s);
                        return JsonUtil.fromJson(s, responseType);
                    });
                }
            }
            return dataMono.map(recvBuf -> {
                try (var in = new ByteBufInputStream(recvBuf)) {
                    return JsonUtil.readJson(in, responseType);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

    }


}
