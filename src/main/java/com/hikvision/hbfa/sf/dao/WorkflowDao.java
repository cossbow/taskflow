package com.hikvision.hbfa.sf.dao;

import com.hikvision.hbfa.sf.entity.Workflow;
import com.hikvision.hbfa.sf.entity.WorkflowNodeEdge;
import com.hikvision.hbfa.sf.entity.WorkflowNodeIo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkflowDao {

    long insert(Workflow workflow);

    int update(Workflow workflow);

    Workflow delete(long id);

    Workflow get(long id);

    Workflow find(@Param("name") String name);


    List<Workflow> list(@Param("lastId") long lastId,
                        @Param("limit") int limit);

    //

    int setIO(WorkflowNodeIo io);

    int delAllIO(@Param("workflowId") long workflowId);

    List<WorkflowNodeIo> listIO(long workflowId);

    //

    int putEdge(WorkflowNodeEdge edge);

    int delAllEdge(@Param("workflowId") long workflowId);

    List<WorkflowNodeEdge> listEdge(long workflowId);


}
