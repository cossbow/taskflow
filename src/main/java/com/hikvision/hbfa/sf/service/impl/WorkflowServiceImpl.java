package com.hikvision.hbfa.sf.service.impl;

import com.hikvision.hbfa.sf.dag.DAGGraph;
import com.hikvision.hbfa.sf.dao.NodeDao;
import com.hikvision.hbfa.sf.dao.WorkflowDao;
import com.hikvision.hbfa.sf.dto.WorkflowDto;
import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.Workflow;
import com.hikvision.hbfa.sf.entity.WorkflowNodeEdge;
import com.hikvision.hbfa.sf.entity.WorkflowNodeIo;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.ex.NotFoundException;
import com.hikvision.hbfa.sf.handler.call.CallableManager;
import com.hikvision.hbfa.sf.handler.dag.data.DAGWorkflow;
import com.hikvision.hbfa.sf.service.WorkflowService;
import com.hikvision.hbfa.sf.util.*;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class WorkflowServiceImpl implements WorkflowService {

    @Autowired
    NodeDao nodeDao;
    @Autowired
    WorkflowDao workflowDao;

    @Autowired
    SimpleCache<Object, DAGWorkflow> workflowCache;

    @Autowired
    CallableManager callableManager;

    //

    private void copyTo(WorkflowDto dto, Workflow workflow) {
        workflow.setOutputParameters(new ObjectMap(dto.getOutputParameters()));

        if (null != dto.getNotifier()) {
            callableManager.get(dto.getNotifier());
            workflow.setNotifier(dto.getNotifier());
        }

        var notifierConfig = dto.getNotifierConfig();
        if (notifierConfig != null)
            workflow.setNotifierConfig(JsonUtil.toJson(notifierConfig));
        workflow.setTimeout(dto.getTimeout());
        workflow.setRemark(dto.getRemark());
    }

    private Workflow toEntity(WorkflowDto dto) {
        var workflow = new Workflow();
        copyTo(dto, workflow);
        workflow.setName(dto.getName());
        return workflow;
    }

    //

    private void updateCache(Workflow workflow) {
        var ioList = workflowDao.listIO(workflow.getId());
        var nodeIds = ioList.stream().map(WorkflowNodeIo::getNodeId).collect(Collectors.toList());
        var nodes = nodeDao.gets(nodeIds);
        var nodeMap = MapUtil.toLongMap(nodes, Node::getId);

        var cachedIOs = new ArrayList<DAGWorkflow.DAGNodeIo>(ioList.size());
        for (var io : ioList) {
            var node = nodeMap.get(io.getNodeId());
            cachedIOs.add(new DAGWorkflow.DAGNodeIo(
                    node.getName(),
                    node.getDefaultArguments(),
                    io.getInputParameters(),
                    io.isIgnoreError()));
        }

        var edges = workflowDao.listEdge(workflow.getId());
        var cachedEdges = new ArrayList<Map.Entry<String, String>>(edges.size());
        for (var edge : edges) {
            cachedEdges.add(Map.entry(
                    nodeMap.get(edge.getFromNodeId()).getName(),
                    nodeMap.get(edge.getToNodeId()).getName()));
        }

        var cw = new DAGWorkflow(workflow, cachedIOs, cachedEdges);
        workflowCache.set(cw.name(), cw);
        workflowCache.set(cw.id(), cw);
    }

    /**
     * 检测是否有环
     */
    private void checkCyclicGraph(WorkflowDto dto) {
        var names = new ArrayList<String>(dto.getNodes().size());
        for (var wn : dto.getNodes()) {
            names.add(wn.getNodeName());
        }
        var edges = new ArrayList<Map.Entry<String, String>>(dto.getEdges().size());
        for (var we : dto.getEdges()) {
            edges.add(Map.entry(we.getFromNode(), we.getToNode()));
        }
        new DAGGraph<>(names, edges);
    }

    private Workflow get(long id, String name) {
        if (id > 0) {
            return workflowDao.get(id);
        }
        if (null != name) {
            return workflowDao.find(name);
        }
        throw new NotFoundException();
    }

    private Map<String, Node> getNodeNameMap(List<WorkflowDto.NodeIo> nodeIos) {
        var nodeNames = new HashSet<String>(nodeIos.size());
        for (var nodeIo : nodeIos) {
            nodeNames.add(nodeIo.getNodeName());
        }
        if (nodeNames.size() != nodeIos.size()) {
            throw new IllegalArgumentException("Node重复");
        }
        var nodes = nodeDao.getByNames(nodeNames);
        var namedMap = new HashMap<String, Node>(nodes.size());
        for (Node n : nodes) {
            namedMap.put(n.getName(), n);
        }
        if (namedMap.size() != nodeNames.size()) {
            var subtract = ValueUtil.subtract(nodeNames, namedMap.keySet());
            wrongNode(subtract);
        }
        return namedMap;
    }

    private void wrongNode(Object name) throws IllegalArgumentException {
        throw new IllegalArgumentException("Node " + name + " 未创建或未绑定");
    }

    @Override
    public long saveWorkflow(WorkflowDto dto) {
        checkCyclicGraph(dto);

        var nodeMap = getNodeNameMap(dto.getNodes());
        for (WorkflowDto.NodeEdge edge : dto.getEdges()) {
            if (!nodeMap.containsKey(edge.getFromNode()))
                wrongNode(edge.getFromNode());
            if (!nodeMap.containsKey(edge.getToNode()))
                wrongNode(edge.getToNode());
        }

        long workflowId;
        var workflow = workflowDao.find(dto.getName());
        if (null != workflow) {
            workflowId = workflow.getId();
            copyTo(dto, workflow);
            workflowDao.update(workflow);
        } else {
            workflow = toEntity(dto);
            workflowId = workflowDao.insert(workflow);
            workflow.setId(workflowId);
        }

        var io = new WorkflowNodeIo();
        io.setWorkflowId(workflowId);
        var edge = new WorkflowNodeEdge();
        edge.setWorkflowId(workflowId);

        workflowDao.delAllIO(workflowId);
        for (var dtoIo : dto.getNodes()) {
            var node = nodeMap.get(dtoIo.getNodeName());
            io.setNodeId(node.getId());
            io.setInputParameters(dtoIo.getInputParameters());
            io.setIgnoreError(dtoIo.isIgnoreError());
            workflowDao.setIO(io);
        }

        workflowDao.delAllEdge(workflowId);
        for (var dtoEdge : dto.getEdges()) {
            edge.setFromNodeId(nodeMap.get(dtoEdge.getFromNode()).getId());
            edge.setToNodeId(nodeMap.get(dtoEdge.getToNode()).getId());
            workflowDao.putEdge(edge);
        }

        updateCache(workflow);

        return workflowId;
    }

    @Override
    public void delWorkflow(long id) {
        var workflow = workflowDao.delete(id);
        if (null != workflow) {
            workflowCache.del(workflow.getId());
            workflowCache.del(workflow.getName());
        }
    }

    @Override
    public List<WorkflowDto> listWorkflow(long lastId, int limit) {
        var workflows = workflowDao.list(lastId, limit);
        var dtoList = new ArrayList<WorkflowDto>(workflows.size());
        for (Workflow workflow : workflows) {
            dtoList.add(fromEntity(workflow));
        }
        return dtoList;
    }

    private LongObjectMap<Node> getNodeIdMap(List<WorkflowNodeIo> ioList) {
        var ids = new ArrayList<Long>();
        for (WorkflowNodeIo io : ioList) {
            ids.add(io.getNodeId());
        }
        var nodes = nodeDao.gets(ids);
        var nodeMap = new LongObjectHashMap<Node>(nodes.size());
        for (Node node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    @Override
    public WorkflowDto getWorkflow(long id, String name) {
        var workflow = get(id, name);
        if (null == workflow) throw new NotFoundException();

        var dto = fromEntity(workflow);

        var ioList = workflowDao.listIO(workflow.getId());
        var nodeMap = getNodeIdMap(ioList);

        var dtoIoList = new ArrayList<WorkflowDto.NodeIo>(ioList.size());
        dto.setNodes(dtoIoList);
        for (var io : ioList) {
            var ioDto = new WorkflowDto.NodeIo();
            ioDto.setNodeId(io.getNodeId());
            ioDto.setInputParameters(io.getInputParameters());
            ioDto.setIgnoreError(io.isIgnoreError());
            ioDto.setCreatedAt(io.getCreatedAt());
            ioDto.setUpdatedAt(io.getUpdatedAt());
            ioDto.setIgnoreError(io.isIgnoreError());
            ioDto.setNodeName(nodeMap.get(io.getNodeId()).getName());
            dtoIoList.add(ioDto);
        }

        var edges = workflowDao.listEdge(workflow.getId());
        var edgeDtoList = new ArrayList<WorkflowDto.NodeEdge>(edges.size());
        dto.setEdges(edgeDtoList);
        for (var edge : edges) {
            var edgeDto = new WorkflowDto.NodeEdge();
            edgeDto.setFromNode(nodeMap.get(edge.getFromNodeId()).getName());
            edgeDto.setToNode(nodeMap.get(edge.getToNodeId()).getName());
            edgeDto.setCreatedAt(edge.getCreatedAt());
            edgeDtoList.add(edgeDto);
        }

        return dto;
    }

    private static WorkflowDto fromEntity(Workflow wf) {
        var dto = new WorkflowDto();
        dto.setId(wf.getId());
        dto.setName(wf.getName());
        dto.setOutputParameters(wf.getOutputParameters());
        dto.setNotifier(wf.getNotifier());
        if (null != wf.getNotifierConfig())
            dto.setNotifierConfig(JsonUtil.parseJsonMap(wf.getNotifierConfig()));
        dto.setTimeout(wf.getTimeout());
        dto.setRemark(wf.getRemark());
        dto.setCreatedAt(wf.getCreatedAt());
        dto.setUpdatedAt(wf.getUpdatedAt());
        return dto;
    }


    private Iterable<Workflow> nodeIterable() {
        return new BatchSource<>(
                lastId -> workflowDao.list(lastId, 10),
                Workflow::getId, 0);
    }

    public void initWorkflows() {
        for (Workflow workflow : nodeIterable()) {
            log.debug("get Workflow-{}", workflow.getId());
            updateCache(workflow);
        }
    }

}
