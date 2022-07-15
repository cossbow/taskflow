package com.hikvision.hbfa.sf.dto;

import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ScriptDto {
    @NotNull(message = "condition type require")
    private ScriptType type;
    @NotEmpty(message = "condition script is empty")
    private String text;

}
