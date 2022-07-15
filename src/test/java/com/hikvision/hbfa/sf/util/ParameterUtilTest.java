package com.hikvision.hbfa.sf.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ParameterUtilTest {

    @Test
    public void testFunJoin() {
        var arguments = Map.of("id", ValueUtil.newUUID(), "name", "ggyy");
        var v = ParameterUtil.readParam(arguments, "${['a',name]|join('-',@)}", String.class);
        Assert.assertEquals("a-ggyy", v);
        v = ParameterUtil.readParam(arguments, "${['a',name,nick] | join('-',@)}", String.class);
        Assert.assertEquals("a-ggyy-", v);
    }

    @Test
    public void replaceParam() {
        var arguments = Map.of(
                "id", 1234,
                "name", "jjj",
                "book", Map.of("id", 554),
                "article", Map.of(
                        "id", 897,
                        "title", "苹果手机📱为什么那么贵？",
                        "time", Instant.now().toString()
                )
        );

        // 获取实参
        var v = ParameterUtil.readParam(arguments, "${price}");
        Assert.assertNull(v);
        v = ParameterUtil.readParam(arguments, "${id");
        Assert.assertEquals("${id", v);
        v = ParameterUtil.readParam(arguments, "${id}");
        Assert.assertEquals(arguments.get("id"), v);
        v = ParameterUtil.readParam(arguments, "${name}");
        Assert.assertEquals(arguments.get("name"), v);
        v = ParameterUtil.readParam(arguments, "${book}");
        Assert.assertSame(arguments.get("book"), v);
        v = ParameterUtil.readParam(arguments, "${shop.id}");
        Assert.assertNull(v);

        var vs = ParameterUtil.readParams(arguments, "${id}", "${name}");
        Assert.assertEquals(arguments.get("id"), vs[0]);
        Assert.assertEquals(arguments.get("name"), vs[1]);

        // 替换形参
        var parameters = Map.of(
                "userid", "${id}",
                "username", "${name}",
                "article", "${article|json(@)}",
                "userInfo", "${{id:id,name:name} | json(@)}",
                "book", "${book}",
                "list", List.of(
                        "${book}",
                        "${article}"
                )
        );
        var result = ParameterUtil.replaceAllParams(parameters, arguments);

        Assert.assertEquals(arguments.get("id"), result.get("userid"));
        Assert.assertEquals(arguments.get("name"), result.get("username"));
        Assert.assertEquals(arguments.get("book"), result.get("book"));

        var list = result.get("list");
        Assert.assertTrue(list instanceof List);
        Assert.assertEquals(arguments.get("book"), ((List<?>) list).get(0));
        Assert.assertEquals(arguments.get("article"), ((List<?>) list).get(1));

        Assert.assertTrue(result.get("article") instanceof String);
        var article = JsonUtil.fromJson((String) (result.get("article")), Map.class);
        @SuppressWarnings("unchecked")
        var artObj = (Map<String, Object>) arguments.get("article");
        Assert.assertEquals(artObj.get("id"), article.get("id"));
        Assert.assertEquals(artObj.get("title"), article.get("title"));
        Assert.assertEquals(artObj.get("time"), article.get("time"));

        Assert.assertTrue(result.get("userInfo") instanceof String);
        var user = JsonUtil.fromJson((String) result.get("userInfo"), Map.class);
        Assert.assertEquals(arguments.get("id"), user.get("id"));
        Assert.assertEquals(arguments.get("name"), user.get("name"));
    }

}
