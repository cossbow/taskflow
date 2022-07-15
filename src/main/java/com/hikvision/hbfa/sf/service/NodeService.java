package com.hikvision.hbfa.sf.service;

import com.hikvision.hbfa.sf.dto.NodeDto;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;

import java.util.List;

public interface NodeService {

    long put(NodeDto dto);

    void del(long id);

    List<NodeDto> list(long lastId, int limit, NodeType type);

}
