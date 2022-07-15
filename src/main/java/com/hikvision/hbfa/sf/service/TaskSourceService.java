package com.hikvision.hbfa.sf.service;

import com.hikvision.hbfa.sf.dto.TaskSourceDto;
import com.hikvision.hbfa.sf.entity.TaskSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TaskSourceService {

    void save(TaskSourceDto dto);

    TaskSourceDto find(String name);

    List<TaskSource> list(int limit, long offset);

    void del(String name);


    void start(String name);

    CompletableFuture<Void> stop(String name);

    TaskSourceDto query(String name);

}
