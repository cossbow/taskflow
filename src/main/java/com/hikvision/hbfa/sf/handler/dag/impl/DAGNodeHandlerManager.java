package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.dag.DAGNodeExecutor;
import com.hikvision.hbfa.sf.util.TypedBeanManager;

import java.util.List;

public class DAGNodeHandlerManager extends TypedBeanManager<NodeType, DAGNodeExecutor> {

    public DAGNodeHandlerManager(List<DAGNodeExecutor> executors) {
        super("DAGNodeExecutors", executors);
    }

}
