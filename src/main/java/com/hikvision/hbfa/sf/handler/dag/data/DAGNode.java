package com.hikvision.hbfa.sf.handler.dag.data;

import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.entity.json.SubmitConfig;
import com.hikvision.hbfa.sf.util.MapUtil;

import java.util.Map;
import java.util.Objects;

/**
 * @see Node
 */
public class DAGNode {
    private final Long id;
    private final String name;
    private final NodeType type;
    private final String config;
    private final Map<String, Object> defaultArguments;

    private final boolean async;
    private final SubmitConfig submitConfig;

    private final int maxRetries;

    public DAGNode(Node n) {
        id = n.getId();
        name = n.getName();
        type = n.getType();
        config = n.getConfig();
        defaultArguments = MapUtil.copyOf(n.getDefaultArguments());
        async = n.isAsync();
        if (null != n.getSubmitConfig()) {
            submitConfig = n.getSubmitConfig();
        } else {
            submitConfig = new SubmitConfig();
        }
        maxRetries = n.getMaxRetries();
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public NodeType type() {
        return type;
    }

    public String config() {
        return config;
    }

    public Map<String, Object> defaultArguments() {
        return defaultArguments;
    }

    public boolean async() {
        return async;
    }

    public String submitId() {
        return submitConfig.getIdParameter();
    }

    public String submitCode() {
        return submitConfig.getCodeParameter();
    }

    public String submitMsg() {
        return submitConfig.getMsgParameter();
    }

    public long timeout() {
        return submitConfig.getTimeout();
    }

    public ResultCode codeOf(String code) {
        return submitConfig.codeOf(code);
    }

    public int maxRetries() {
        return maxRetries;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DAGNode on = (DAGNode) o;
        return Objects.equals(name, on.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Node(" + name + ')';
    }
}
