package com.hikvision.hbfa.sf.ext.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoiceTaskSubmitResult extends VoiceBaseResult<String> {
    @JsonProperty("taskID")
    private String taskId;

    @Override
    public String data() {
        return taskId;
    }
}
