package com.hikvision.hbfa.sf.handler.call.rest;

import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.ex.HttpStatusException;
import com.hikvision.hbfa.sf.handler.call.CallParam;
import com.hikvision.hbfa.sf.handler.call.CallResult;
import com.hikvision.hbfa.sf.handler.call.Callable;
import com.hikvision.hbfa.sf.handler.call.ConfigCallable;
import com.hikvision.hbfa.sf.util.HttpClientUtil;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class RestCallable
        extends ConfigCallable<RestConfig>
        implements Callable<RestConfig> {

    public RestCallable() {
        super(RestConfig.class);
    }

    @Override
    public CallType type() {
        return CallType.REST;
    }


    private Mono<CallResult> request(CallParam<RestConfig> param) {
        var config = param.getConfig();
        var queries = config.getQueries();
        var input = param.getInput();
        log.debug("call rest[{}]: {}", param.getKey(), JsonUtil.lazyJson(input));

        if (ValueUtil.isEmpty(config.getUri())) {
            return Mono.just(new CallResult(ResultCode.BAD_CONFIG, "url empty"));
        }
        if (null == config.getMethod()) {
            return Mono.just(new CallResult(ResultCode.BAD_CONFIG, "http method empty"));
        }

        String uri;
        if (ValueUtil.isEmpty(queries)) {
            uri = config.getUri();
        } else {
            var ub = new URIBuilder(URI.create(config.getUri()));
            for (var entry : queries.entrySet()) {
                var argument = ParameterUtil.readParam(input, entry.getValue());
                if (null != argument) {
                    ub.addParameter(entry.getKey(), argument.toString());
                } else {
                    log.warn("query-param[{}] not exists", entry.getKey());
                }
            }
            uri = ub.toString();
        }

        Object body = input;
        if (null != config.getBody()) {
            body = ParameterUtil.readParam(input, config.getBody());
        }


        return HttpClientUtil.<ObjectMap>newBuilder()
                .method(config.getMethod()).uri(uri).headers(config.getHeaders())
                .send(body).recv(ObjectMap.class)
                .log(log).tag(config.getUri()).build()
                .map(CallResult.successFun())
                .onErrorResume(e -> {
                    log.error("REST [{}] error", uri);
                    CallResult cr;
                    if (e instanceof TimeoutException || e instanceof ConnectTimeoutException) {
                        cr = new CallResult(ResultCode.NETWORK, e.getMessage());
                    } else if (e instanceof HttpStatusException) {
                        cr = new CallResult(ResultCode.BAD_REQUEST, e.getMessage());
                    } else {
                        return Mono.error(e);
                    }
                    return Mono.just(cr);
                }).defaultIfEmpty(new CallResult());
    }


    @Override
    public CompletableFuture<CallResult> doAsyncCall(CallParam<RestConfig> param) {
        return request(param).toFuture();
    }
}
