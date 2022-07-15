package com.hikvision.hbfa.sf.entity.json;

import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import lombok.Data;

@Data
public class Script {
    private ScriptType type;
    private String text;
}
