package com.hikvision.hbfa.sf.service.impl;

import com.hikvision.hbfa.sf.dao.NodeDao;
import com.hikvision.hbfa.sf.dao.WorkflowDao;
import com.hikvision.hbfa.sf.dispatch.SubtaskSubmitSubscriber;
import com.hikvision.hbfa.sf.dto.NodeDto;
import com.hikvision.hbfa.sf.dto.ScriptDto;
import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.entity.json.SubmitConfig;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.impl.DAGNodeHandlerManager;
import com.hikvision.hbfa.sf.handler.script.ScriptExecutorsManager;
import com.hikvision.hbfa.sf.service.NodeService;
import com.hikvision.hbfa.sf.util.BatchSource;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.SimpleCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@Service
public class NodeServiceImpl implements NodeService {

    @Autowired
    NodeDao nodeDao;
    @Autowired
    WorkflowDao workflowDao;

    @Autowired
    SimpleCache<Object, DAGNode> nodeCache;

    @Autowired
    ScriptExecutorsManager scriptExecutorsManager;
    @Autowired
    SubtaskSubmitSubscriber subtaskSubmitSubscriber;
    @Autowired
    DAGNodeHandlerManager dagNodeHandlerManager;

    //

    private Node toEntity(NodeDto dto) {
        var n = new Node();
        n.setName(dto.getName());

        dagNodeHandlerManager.get(dto.getType());
        n.setType(dto.getType());

        var submitConfig = dto.getSubmitConfig();
        if (null == submitConfig) {
            submitConfig = new NodeDto.SubmitConfigDto();
        }

        n.setConfig(JsonUtil.toJson(dto.getConfig()));
        n.setDefaultArguments(dto.getDefaultArguments());

        n.setAsync(dto.isAsync());
        var sc = new SubmitConfig();
        sc.setCodeMap(submitConfig.getCodeMap());
        sc.setDefaultCode(submitConfig.getDefaultCode());
        var parameters = submitConfig.getParameters();
        if (null != parameters) {
            sc.setIdParameter(parameters.getId());
            sc.setCodeParameter(parameters.getCode());
            sc.setMsgParameter(parameters.getMsg());
        }
        sc.setTimeout(submitConfig.getTimeout());
        n.setSubmitConfig(sc);

        n.setMaxRetries(dto.getMaxRetries());
        n.setRemark(dto.getRemark());
        return n;
    }

    static Script toEntity(ScriptDto dto) {
        var c = new Script();
        c.setType(dto.getType());
        c.setText(dto.getText());
        return c;
    }

    static NodeDto fromEntity(Node n) {
        var d = new NodeDto();
        d.setId(n.getId());
        d.setName(n.getName());
        d.setType(n.getType());
        d.setConfig(JsonUtil.parseJsonMap(n.getConfig()));
        d.setDefaultArguments(n.getDefaultArguments());

        d.setAsync(n.isAsync());
        var sc = n.getSubmitConfig();
        var scDto = new NodeDto.SubmitConfigDto();
        scDto.setCodeMap(sc.getCodeMap());
        scDto.setDefaultCode(sc.getDefaultCode());
        var parameters = new NodeDto.Parameters();
        scDto.setParameters(parameters);
        parameters.setId(sc.getIdParameter());
        parameters.setCode(sc.getCodeParameter());
        parameters.setMsg(sc.getMsgParameter());
        scDto.setTimeout(sc.getTimeout());
        d.setSubmitConfig(scDto);

        d.setMaxRetries(n.getMaxRetries());
        d.setRemark(n.getRemark());
        d.setCreatedAt(n.getCreatedAt());
        d.setUpdatedAt(n.getUpdatedAt());
        return d;
    }


    //

    @Override
    public long put(NodeDto nodeDto) {
        var node = toEntity(nodeDto);
        var entity = nodeDao.find(node.getName());
        Node newNode;
        if (null != entity) {
            node.setId(entity.getId());
            newNode = nodeDao.update(node);
        } else {
            newNode = nodeDao.insert(node);
        }

        if (NodeType.SCRIPT.equals(newNode.getType())) {
            scriptExecutorsManager.delFunctionCache(newNode.getId());
        }

        // 更新缓存
        updateCache(newNode);
        // 检查kafka提交的节点并订阅
        subtaskSubmitSubscriber.updateSubscribe(newNode, false);

        return newNode.getId();
    }


    @Override
    public void del(long id) {
        var node = nodeDao.delete(id);
        if (null == node) return;
        nodeCache.del(node.getId());
        nodeCache.del(node.getName());
        subtaskSubmitSubscriber.updateSubscribe(node, true);
    }

    @Override
    public List<NodeDto> list(long lastId, int limit, NodeType type) {
        var nodes = nodeDao.list(lastId, limit, type);
        var dtoList = new ArrayList<NodeDto>(nodes.size());
        for (Node node : nodes) {
            dtoList.add(fromEntity(node));
        }
        return dtoList;
    }


    private Iterable<Node> nodeIterable() {
        return new BatchSource<>(
                lastId -> nodeDao.list(lastId, 10, null),
                Node::getId, 0);
    }

    public void initNodes() {
        for (Node node : nodeIterable()) {
            log.debug("get Node-{}", node.getName());
            updateCache(node);
            subtaskSubmitSubscriber.updateSubscribe(node, false);
        }
    }

    private void updateCache(Node node) {
        var cachedNode = new DAGNode(node);
        log.debug("cache Node-{}", cachedNode.name());
        nodeCache.set(cachedNode.id(), cachedNode);
        nodeCache.set(cachedNode.name(), cachedNode);
    }

}
