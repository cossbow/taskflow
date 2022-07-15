package com.hikvision.hbfa.sf;

/**
 * 子任务提交类型，异步子任务完成时提交到本地
 */
public enum Submit {
    // 通过Rest接口提交
    REST,
    // 通过kafka提交
    KAFKA,
}
