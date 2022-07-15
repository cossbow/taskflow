package com.cossbow.task;

import org.springframework.core.ResolvableType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

public class HttpSomeTest {
    public static void main(String[] args) throws Exception {
        var ubf = new DefaultUriBuilderFactory();
        var builder = ubf.builder();
        builder.queryParam("verbose", true);
        builder.host("localhost").port(9457).scheme("http").pathSegment("v1", "monitor", "stats");
        System.out.println(builder.build());

        var wc = WebClient.builder().uriBuilderFactory(ubf).build();
        var uri = "http://localhost:12306/cookie/rand";
        var o = wc.get().uri(uri).httpRequest(request -> {
            System.out.println("before request");
        }).exchangeToMono(response -> {
            System.out.println("after request");
            response.cookies().forEach((k, cookies) -> {
                System.out.println(k + ": " + cookies.get(0));
            });
            return response.body(BodyExtractors.toMono(String.class));
        }).block();
        System.out.println(o.substring(0, 10));

    }

}
