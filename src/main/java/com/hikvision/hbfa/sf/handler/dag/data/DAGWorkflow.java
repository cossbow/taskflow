package com.hikvision.hbfa.sf.handler.dag.data;

import com.hikvision.hbfa.sf.dag.DAGGraph;
import com.hikvision.hbfa.sf.entity.Workflow;
import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.util.MapUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class DAGWorkflow implements Function<String, DAGWorkflow.DAGNodeIo> {
    private final Long id;
    private final String name;

    private final Map<String, Object> outputParameters;

    private final CallType notifier;
    private final String notifierConfig;

    private final long timeout;

    private final DAGGraph<String> graph;

    private final Map<String, DAGNodeIo> ioMap;

    public DAGWorkflow(Workflow w,
                       List<DAGNodeIo> ios,
                       List<Map.Entry<String, String>> edges) {
        id = w.getId();
        name = w.getName();
        outputParameters = MapUtil.copyOf(w.getOutputParameters());
        notifier = w.getNotifier();
        notifierConfig = w.getNotifierConfig();
        timeout = w.getTimeout();

        var ioMap = new HashMap<String, DAGNodeIo>(ios.size());
        for (var io : ios) {
            ioMap.put(io.nodeName(), io);
        }
        this.ioMap = ioMap;
        this.graph = new DAGGraph<>(ioMap.keySet(), edges);
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Map<String, Object> outputParameters() {
        return outputParameters;
    }

    public CallType notifier() {
        return notifier;
    }

    public String notifierConfig() {
        return notifierConfig;
    }

    public long timeout() {
        return timeout;
    }


    public DAGGraph<String> graph() {
        return graph;
    }


    public DAGNodeIo apply(String nodeName) {
        return ioMap.get(nodeName);
    }


    @Override
    public String toString() {
        return "Workflow(" + name + ')';
    }

    public static class DAGNodeIo {
        private final String nodeName;
        private final Map<String, Object> inputParameters;
        private final boolean ignoreError;

        public DAGNodeIo(String nodeName,
                         Map<String, Object> defaultArguments,
                         Map<String, Object> inputParameters,
                         boolean ignoreError) {
            this.nodeName = nodeName;
            if (null == defaultArguments) {
                if (null == inputParameters) {
                    this.inputParameters = null;
                } else {
                    this.inputParameters = Map.copyOf(inputParameters);
                }
            } else {
                if (null == inputParameters) {
                    this.inputParameters = Map.copyOf(defaultArguments);
                } else {
                    var parameters = new HashMap<>(defaultArguments);
                    MapUtil.deepMerge(parameters, inputParameters);
                    this.inputParameters = MapUtil.copyOf(parameters);
                }
            }
            this.ignoreError = ignoreError;
        }

        public String nodeName() {
            return nodeName;
        }

        public Map<String, Object> inputParameters() {
            return inputParameters;
        }

        public boolean ignoreError() {
            return ignoreError;
        }

    }
}
