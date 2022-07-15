package com.hikvision.hbfa.sf.ext.voice;

import com.hikvision.hbfa.sf.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.hikvision.hbfa.sf.ext.voice.VoiceTaskData.AsrRst;

public class VoiceAnalysisTask {

    private final String subtaskId;
    private final String taskId;


    private final List<AsrRst> contents = new ArrayList<>();

    private volatile int progress = 0;

    public VoiceAnalysisTask(String subtaskId, String taskId) {
        this.subtaskId = Objects.requireNonNull(subtaskId);
        this.taskId = Objects.requireNonNull(taskId);
    }

    public String getSubtaskId() {
        return subtaskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void addContent(AsrRst content) {
        synchronized (contents) {
            contents.add(content);
        }
    }

    public List<Map<?, ?>> getContents() {
        synchronized (contents) {
            return contents.stream().map(ar -> Map.of(
                    "startTime", ar.getStartTime(),
                    "endTime", ValueUtil.nullIf(ar.getEndTime(), ""),
                    "audioContent", ar.getAudioContent()
            )).collect(Collectors.toList());
        }
    }

}
