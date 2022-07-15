package com.hikvision.hbfa.sf.service;

import com.hikvision.hbfa.sf.dto.WorkflowDto;

import java.util.List;

public interface WorkflowService {

    long saveWorkflow(WorkflowDto dto);

    void delWorkflow(long id);

    List<WorkflowDto> listWorkflow(long lastId, int limit);

    WorkflowDto getWorkflow(long id, String name);

}
