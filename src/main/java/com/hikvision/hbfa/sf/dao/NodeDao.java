package com.hikvision.hbfa.sf.dao;

import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface NodeDao {

    Node insert(Node node);

    Node update(Node node);

    Node delete(long id);

    Node find(String name);

    Node get(long id);

    List<Node> gets(Collection<Long> ids);

    List<Node> getByNames(Collection<String> names);

    List<Node> list(@Param("lastId") long lastId,
                    @Param("limit") int limit,
                    @Param("type") NodeType type);

}
