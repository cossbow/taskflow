package com.hikvision.hbfa.sf.service.impl;

import com.hikvision.hbfa.sf.dao.TaskSourceDao;
import com.hikvision.hbfa.sf.dispatch.TaskSourceDispatcherService;
import com.hikvision.hbfa.sf.dto.TaskSourceDto;
import com.hikvision.hbfa.sf.entity.TaskSource;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;
import com.hikvision.hbfa.sf.ex.NotFoundException;
import com.hikvision.hbfa.sf.service.TaskSourceService;
import com.hikvision.hbfa.sf.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TaskSourceServiceImpl implements TaskSourceService {

    @Autowired
    TaskSourceDao taskSourceDao;

    @Autowired
    TaskSourceDispatcherService dispatcherService;


    private TaskSource toEntity(TaskSourceDto dto) {
        var entity = new TaskSource();
        entity.setName(dto.getName());
        entity.setWorkflow(dto.getWorkflow());

        dispatcherService.get(dto.getType());
        entity.setType(dto.getType());

        entity.setConfig(JsonUtil.toJson(dto.getConfig()));
        entity.setBatch(dto.getBatch());
        entity.setTimeout(dto.getTimeout());
        entity.setRetries(dto.getRetries());
        entity.setAutostart(dto.isAutostart());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    @Transactional
    @Override
    public void save(TaskSourceDto dto) {
        var entity = toEntity(dto);
        var old = taskSourceDao.find(dto.getName());
        if (old != null) {
            entity.setId(old.getId());
            taskSourceDao.update(entity);
        } else {
            taskSourceDao.insert(entity);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public TaskSourceDto find(String name) {
        var entity = taskSourceDao.find(name);
        if (null == entity) {
            throw new NotFoundException();
        }
        var dto = new TaskSourceDto();
        dto.setName(entity.getName());
        dto.setWorkflow(entity.getWorkflow());
        dto.setType(entity.getType());
        dto.setConfig(JsonUtil.parseJsonMap(entity.getConfig()));
        dto.setBatch(entity.getBatch());
        dto.setTimeout(entity.getTimeout());
        dto.setRetries(entity.getRetries());
        dto.setAutostart(entity.isAutostart());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<TaskSource> list(int limit, long offset) {
        return taskSourceDao.list(limit, offset, null);
    }

    @Transactional
    @Override
    public void del(String name) {
        var status = dispatcherService.status(name);
        if (status == TaskSourceStatus.RUNNING) {
            throw new IllegalStateException("TaskSource-" + name + " is RUNNING.");
        } else if (status == TaskSourceStatus.STOPPING) {
            throw new IllegalStateException("TaskSource-" + name + " is STOPPING, wait latter...");
        }
        taskSourceDao.remove(name);
    }

    @Transactional(readOnly = true)
    @Override
    public void start(String name) {
        var source = taskSourceDao.find(name);
        if (null == source) {
            throw new NotFoundException();
        }
        dispatcherService.start(source);
    }

    @Override
    public CompletableFuture<Void> stop(String name) {
        return dispatcherService.stop(name);
    }

    @Transactional
    @Override
    public TaskSourceDto query(String name) {
        var dto = find(name);
        var status = dispatcherService.status(name);
        dto.setStatus(status);
        return dto;
    }

    public void initTaskSources() {
        final int limit = 10;
        int offset = 0;
        while (true) {
            var list = taskSourceDao.list(limit, offset, Boolean.TRUE);
            if (list.size() > 0) {
                for (TaskSource source : list) {
                    dispatcherService.start(source);
                }
            }
            if (list.size() < limit) break;
            offset += limit;
        }
    }

}
