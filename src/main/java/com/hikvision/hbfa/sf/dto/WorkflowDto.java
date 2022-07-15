package com.hikvision.hbfa.sf.dto;

import com.hikvision.hbfa.sf.entity.Workflow;
import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @see Workflow
 */
@Data
public class WorkflowDto {
    // view
    private long id;
    // 【工作流名称】
    @NotEmpty
    private String name;

    private Map<String, Object> outputParameters;
    private CallType notifier;
    private Map<String, Object> notifierConfig;
    private long timeout;
    private String remark;


    @NotNull
    @NotEmpty
    private @Valid List<NodeIo> nodes;
    @NotNull
    private @Valid List<NodeEdge> edges;

    // view
    private Instant createdAt;
    private Instant updatedAt;


    @Data
    public static class NodeIo {
        @NotEmpty
        private String nodeName;

        private ObjectMap inputParameters;

        // 忽略此节点的错误，Workflow将正常提交
        private boolean ignoreError;

        private long nodeId;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    public static class NodeEdge {
        @NotEmpty
        private String fromNode;
        @NotEmpty
        private String toNode;

        private Instant createdAt;
    }

}
