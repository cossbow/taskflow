package com.hikvision.hbfa.sf.dto;

import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.entity.json.SubmitConfig;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * @see Node
 */
@Data
public class NodeDto {
    private long id;
    // 【任务名称】
    @NotEmpty
    private String name;
    @NotNull
    private NodeType type;
    @NotNull
    private ObjectMap config;
    private ObjectMap defaultArguments;

    private boolean async;
    private @Valid SubmitConfigDto submitConfig;


    @Min(0)
    private int maxRetries;
    private String remark;


    private Instant createdAt;
    private Instant updatedAt;


    /**
     * @see SubmitConfig
     */
    @Data
    public static class SubmitConfigDto {
        private Map<String, ResultCode> codeMap;
        private ResultCode defaultCode = ResultCode.SUCCESS;
        private Parameters parameters;
        private long timeout;
    }

    @Data
    public static class Parameters {
        private String id;
        private String code;
        private String msg;
    }

}
