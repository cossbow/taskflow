package com.hikvision.hbfa.sf.entity.json;

import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.util.ThrowsUtil;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>提交相关的配置</h2>
 */
@Data
public class SubmitConfig implements Cloneable {
    // 结果状态码表：将第三方的状态码映射成本地状态码，为空则默用DEFAULT_CODES
    private Map<String, ResultCode> codeMap;
    // 状态码表查不到时的默认状态
    private ResultCode defaultCode = ResultCode.SUCCESS;
    // id形参：通过jsonpath获取Subtask.id
    private String idParameter = "${id}";
    // code形参：通过jsonpath获取第三方的错误码
    private String codeParameter = "${code}";
    // msg形参：通过jsonpath获取第三方的错误信息
    private String msgParameter = "${msg}";
    // 超时时间
    private long timeout;

    //

    public Map<String, ResultCode> noNullCodeMap() {
        return null != codeMap ? codeMap : DEFAULT_CODES;
    }

    public ResultCode defaultCode() {
        return null == defaultCode ? ResultCode.SUCCESS : defaultCode;
    }


    public ResultCode codeOf(String c) {
        if (null == c) return defaultCode();
        var codeMap = noNullCodeMap();
        if (null == codeMap || codeMap.isEmpty())
            return defaultCode();
        return codeMap.getOrDefault(c, defaultCode());
    }

    @Override
    public SubmitConfig clone() {
        try {
            var nc = (SubmitConfig) super.clone();
            if (null != codeMap) {
                nc.setCodeMap(Map.copyOf(codeMap));
            }
            return nc;
        } catch (CloneNotSupportedException e) {
            // 不会发生的异常
            throw ThrowsUtil.unchecked(e);
        }
    }


    //


    public static final Map<String, ResultCode> DEFAULT_CODES;

    static {
        var values = ResultCode.values();
        var codes = new HashMap<String, ResultCode>(values.length);
        for (ResultCode value : values) {
            codes.put(value.name(), value);
        }
        DEFAULT_CODES = Map.copyOf(codes);
    }

}
