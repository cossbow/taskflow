package com.hikvision.hbfa.sf.controller;

import com.hikvision.hbfa.sf.config.SubmitManager;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(ParameterUtil.VERSION + "/monitor")
public class MonitorController {

    @Autowired
    SubmitManager submitManager;
    @Autowired
    DAGTaskService dagTaskService;

    @GetMapping("/stats")
    public Object stats(@RequestParam(required = false) boolean verbose) {
        var data = dagTaskService.stats(verbose);
        return BaseResult.success(data);
    }

    @GetMapping("/config")
    public Object config() {
        var submit = submitManager.submitArguments(0, "");
        return BaseResult.success(Map.of("submit", submit));
    }

}
