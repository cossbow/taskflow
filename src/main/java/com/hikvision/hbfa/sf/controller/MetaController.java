package com.hikvision.hbfa.sf.controller;

import com.hikvision.hbfa.sf.dto.NodeDto;
import com.hikvision.hbfa.sf.dto.TaskSourceDto;
import com.hikvision.hbfa.sf.dto.WorkflowDto;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.service.NodeService;
import com.hikvision.hbfa.sf.service.TaskSourceService;
import com.hikvision.hbfa.sf.service.WorkflowService;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;


@Validated
@RequestMapping(ParameterUtil.VERSION + "/meta")
@RestController
public class MetaController {

    @Autowired
    NodeService nodeService;
    @Autowired
    WorkflowService workflowService;
    @Autowired
    TaskSourceService taskSourceService;

    @PostMapping("/node")
    public BaseResult<?> saveNode(@RequestBody @Valid NodeDto dto) {
        ParameterUtil.illegalName(dto.getName());

        var id = nodeService.put(dto);
        return BaseResult.success(Map.of("id", id));
    }

    @PostMapping("/nodes")
    public BaseResult<?> saveNodes(@RequestBody @Valid NodeDto[] list) {
        for (var dto : list) {
            ParameterUtil.illegalName(dto.getName());
        }
        var idMap = new HashMap<String, Long>(list.length);
        for (var dto : list) {
            var id = nodeService.put(dto);
            idMap.put(dto.getName(), id);
        }
        return BaseResult.success(idMap);
    }

    @DeleteMapping("/node/{id}")
    public BaseResult<?> delNode(@PathVariable long id) {
        nodeService.del(id);
        return BaseResult.success();
    }

    @GetMapping("/nodes")
    public BaseResult<?> listNode(@RequestParam(name = "lastId", defaultValue = "0") long lastId,
                                  @RequestParam(name = "limit", defaultValue = "20") int limit,
                                  @RequestParam(name = "type", required = false) NodeType type) {
        var list = nodeService.list(lastId, limit, type);
        return BaseResult.success(list);
    }

    @PostMapping("/workflow")
    public BaseResult<?> saveWorkflow(@RequestBody @Valid WorkflowDto workflowDto) {
        ParameterUtil.illegalName(workflowDto.getName());
        var id = workflowService.saveWorkflow(workflowDto);
        return BaseResult.success(Map.of("id", id));
    }

    @DeleteMapping("/workflow")
    public BaseResult<?> delWorkflow(long id) {
        workflowService.delWorkflow(id);
        return BaseResult.success();
    }

    @GetMapping("/workflows")
    public BaseResult<?> listWorkflow(@RequestParam(name = "lastId", defaultValue = "0") long lastId,
                                      @RequestParam(name = "limit", defaultValue = "20") int limit) {
        var list = workflowService.listWorkflow(lastId, limit);
        return BaseResult.success(list);
    }

    @GetMapping("/workflow/{key}")
    public BaseResult<WorkflowDto> getWorkflow(@PathVariable String key) {
        long id = 0;
        String name = null;
        try {
            id = Long.parseLong(key);
        } catch (NumberFormatException e) {
            name = key;
        }
        var dto = workflowService.getWorkflow(id, name);
        return BaseResult.success(dto);
    }


    @PostMapping("/taskSource")
    public BaseResult<?> saveTaskSource(@RequestBody @Valid TaskSourceDto dto) {
        taskSourceService.save(dto);
        return BaseResult.success();
    }

    @DeleteMapping("/taskSource/{name}")
    public BaseResult<?> delTaskSource(@PathVariable String name) {
        taskSourceService.del(name);
        return BaseResult.success();
    }

    @GetMapping("/taskSource/{name}")
    public BaseResult<?> queryTaskSource(@PathVariable String name) {
        var dto = taskSourceService.query(name);
        return BaseResult.success(dto);
    }

    @GetMapping("/taskSources")
    public BaseResult<?> listTaskSource(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") long offset) {
        var list = taskSourceService.list(limit, offset);
        return BaseResult.success(list);
    }

}
