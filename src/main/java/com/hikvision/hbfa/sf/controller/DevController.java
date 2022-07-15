package com.hikvision.hbfa.sf.controller;

import com.hikvision.hbfa.sf.dto.ScriptDto;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.handler.script.ScriptExecutorsManager;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Validated
@RequestMapping(ParameterUtil.VERSION + "/dev")
@RestController
public class DevController {

    @Autowired
    ScriptExecutorsManager scriptExecutorsManager;

    @Data
    public static class TestJsonpathDto {
        @NotEmpty
        private String jsonpath;
        @NotNull
        private ObjectMap arguments;
    }

    @PostMapping("jsonpath")
    public Object jsonpath(@RequestBody @Valid TestJsonpathDto dto) {
        return ParameterUtil.testJsonpath(dto.arguments, dto.jsonpath);
    }

    @Data
    public static class TestScriptDto {
        @NotNull
        private ScriptDto script;
        @NotNull
        private ObjectMap input;

        public Script script() {
            var s = new Script();
            s.setType(script.getType());
            s.setText(script.getText());
            return s;
        }
    }

    @PostMapping("test-condition")
    public Object testCondition(@RequestBody @Valid TestScriptDto dto) {
        return scriptExecutorsManager.checkConditionSyntax(dto.script(), dto.input);
    }

    @PostMapping("test-function")
    public Object testFunction(@RequestBody @Valid TestScriptDto dto) {
        return scriptExecutorsManager.checkFunctionSyntax(dto.script(), dto.input);
    }


}
