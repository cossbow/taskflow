package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.entity.TaskSource;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import com.hikvision.hbfa.sf.util.TypedBean;

public interface TaskSourceDispatcherFactory extends TypedBean<TaskSourceType> {

    TaskSourceDispatcher create(
            SourceParam config,
            TaskSourceReceiver receiver);

    default TaskSourceDispatcher create(
            TaskSource src,
            TaskSourceReceiver receiver) {
        var param = new SourceParam(src.getName(), src.getWorkflow(), src.getConfig(),
                src.getBatch(), src.getTimeout(), src.getRetries());
        return create(param, receiver);
    }

}
