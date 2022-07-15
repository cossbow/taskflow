package com.hikvision.hbfa.sf.dao;

import com.hikvision.hbfa.sf.entity.TaskSource;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskSourceDao {

    void insert(TaskSource source);

    void update(TaskSource source);

    void remove(String name);

    TaskSource find(String name);

    List<TaskSource> list(@Param("limit") int limit,
                          @Param("offset") long offset,
                          @Param("autostart") Boolean autostart);

}
