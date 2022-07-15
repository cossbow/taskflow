package com.hikvision.hbfa.sf.ext.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 未使用的字段不解析
 */
@Data
public class VoiceTaskData {

    private VoiceResult voiceResult;


    // 下面两个属性用于内部结束任务的
    private boolean voiceAnalysisFinished;
    private String voiceAnalysisTaskId;
    private String voiceAnalysisNodeName;


    @EqualsAndHashCode(callSuper = true)
    @Data
    static class VoiceResult extends VoiceBaseResult<Void> {
        private TargetAttrs targetAttrs;
        private AsrRst asrRst;
    }

    @Data
    static class AsrRst {
        // 开始时间：用于排序，非空
        private String startTime;
        private String endTime;
        // 识别文本内容：非空
        private String audioContent;
    }

    @Data
    static class TargetAttrs {
        @JsonProperty("taskID")
        private String taskId;
    }

}
