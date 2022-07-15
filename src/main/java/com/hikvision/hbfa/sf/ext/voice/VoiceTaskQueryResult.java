package com.hikvision.hbfa.sf.ext.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoiceTaskQueryResult
        extends VoiceBaseResult<List<VoiceTaskQueryResult.TaskStatus>> {

    private List<TaskStatus> status;

    @Override
    public List<TaskStatus> data() {
        return status;
    }

    /**
     * 必填,任务状态: integer
     * 1-未调度      Not Dispatched
     * 2-等待        Waiting
     * 3-正在执行    Executing
     * 4-已完成      Completed
     * 5-已删除      Deleted
     * 6-节点不在线  Offline
     * 7-服务器宕机  Server Down
     * 8-正在停止    Stopping
     * 9-已停止      Stopped
     * 10-节点重启    Rebooting
     * 11-暂停        Paused
     * 12-暂停中      Pausing
     * 13-任务失败    Failed
     * 14-taskID不存在
     */
    @Data
    static class TaskStatus {
        @JsonProperty("taskID")
        private String taskId;
        private String streamType;
        private int taskStatus;
        private int process;
        private String isDemonstrating;
        private String taskCode;
        private String taskFinishTime;
        private String startTime;
        private String endTime;
        private String videoSource;
    }

}
