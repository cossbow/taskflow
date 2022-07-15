package com.hikvision.hbfa.sf.handler.call;

import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import lombok.Data;

import java.util.Map;
import java.util.function.Function;

@Data
public class CallResult {
    private ResultCode code;
    private String msg;
    private Map<String, Object> data;

    public CallResult() {
        this.code = ResultCode.SUCCESS;
    }

    public CallResult(Map<String, Object> data) {
        this.code = ResultCode.SUCCESS;
        this.data = data;
    }

    public CallResult(ResultCode code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    //

    public static Function<Map<String, Object>, CallResult> successFun() {
        return CallResult::new;
    }

}
